package com.example.way.ui.walk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.way.R
import com.example.way.data.remote.GeoapifyPlace
import com.example.way.data.remote.GeoapifyService
import com.example.way.data.repository.LocationRepository
import com.example.way.databinding.FragmentWalkDestinationBinding
import com.example.way.service.WalkForegroundService
import com.example.way.service.WalkSessionManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalkDestinationFragment : Fragment() {

    private var _binding: FragmentWalkDestinationBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var walkSessionManager: WalkSessionManager
    @Inject lateinit var locationRepository: LocationRepository

    private lateinit var savedLocationsAdapter: FrequentLocationAdapter
    private var placesAdapter: PlacePredictionAdapter? = null
    private var searchJob: Job? = null
    private var selectedFromSearch = false
    private var gpsBias: Pair<Double, Double>? = null

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (locationGranted) {
            startWalk()
        } else {
            Toast.makeText(requireContext(), "Location permission is required for walk monitoring", Toast.LENGTH_LONG).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestCurrentLocation(showFeedback = true)
        } else {
            Toast.makeText(requireContext(), "Location permission is needed for nearby place search", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalkDestinationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (GeoapifyService.isConfigured) {
            setupAutocomplete()
        } else {
            binding.btnUseGps.visibility = View.GONE
            binding.tvGpsStatus.text = "Geoapify key missing. Add GEOAPIFY_API_KEY in local.properties"
        }

        setupSavedLocations()

        if (hasLocationPermission()) {
            requestCurrentLocation(showFeedback = false)
        }

        binding.btnUseGps.setOnClickListener {
            if (hasLocationPermission()) {
                requestCurrentLocation(showFeedback = true)
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        binding.btnStartWalk.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                prepareDestinationAndStart()
            }
        }
    }

    private suspend fun prepareDestinationAndStart() {
        val destinationText = binding.etDestination.text.toString().trim()
        if (destinationText.isEmpty()) {
            binding.tilDestination.error = "Please enter a destination"
            return
        }
        binding.tilDestination.error = null

        if (!selectedFromSearch || walkSessionManager.destinationLat == 0.0 || walkSessionManager.destinationLng == 0.0) {
            val resolved = resolveDestination(destinationText)
            if (resolved == null) {
                binding.tilDestination.error = "Please select a valid place from suggestions"
                Toast.makeText(requireContext(), "Could not resolve destination coordinates", Toast.LENGTH_SHORT).show()
                return
            }
            applySelectedPlace(resolved)
        }

        checkPermissionsAndStart()
    }

    private suspend fun resolveDestination(query: String): GeoapifyPlace? {
        if (!GeoapifyService.isConfigured) return null
        val near = gpsBias
        val results = GeoapifyService.autocomplete(
            query = query,
            nearLat = near?.first,
            nearLng = near?.second
        )
        return results.firstOrNull()
    }

    // ── Geoapify Autocomplete ──

    private fun setupAutocomplete() {
        placesAdapter = PlacePredictionAdapter { place ->
            applySelectedPlace(place)
            binding.rvPlacesSuggestions.visibility = View.GONE
            binding.cardSuggestions.visibility = View.GONE
        }

        binding.rvPlacesSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlacesSuggestions.adapter = placesAdapter

        binding.etDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedFromSearch = false
            }
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 3 && !selectedFromSearch) {
                    searchPlaces(query)
                } else {
                    binding.rvPlacesSuggestions.visibility = View.GONE
                    binding.cardSuggestions.visibility = View.GONE
                }
            }
        })
    }

    private fun applySelectedPlace(place: GeoapifyPlace) {
        binding.etDestination.setText(place.fullAddress.ifBlank { place.name })
        binding.etDestination.setSelection(binding.etDestination.text?.length ?: 0)
        selectedFromSearch = true
        walkSessionManager.setDestination(
            name = place.fullAddress.ifBlank { place.name },
            lat = place.latitude,
            lng = place.longitude
        )
        Log.d("Geoapify", "Selected: ${place.name} @ ${place.latitude},${place.longitude}")
    }

    private fun searchPlaces(query: String) {
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(400)
            if (_binding == null) return@launch

            val near = gpsBias
            val results = GeoapifyService.autocomplete(
                query = query,
                nearLat = near?.first,
                nearLng = near?.second
            )

            if (_binding == null) return@launch
            if (results.isNotEmpty()) {
                placesAdapter?.submitList(results)
                binding.rvPlacesSuggestions.visibility = View.VISIBLE
                binding.cardSuggestions.visibility = View.VISIBLE
            } else {
                binding.rvPlacesSuggestions.visibility = View.GONE
                binding.cardSuggestions.visibility = View.GONE
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCurrentLocation(showFeedback: Boolean) {
        if (!hasLocationPermission()) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateGpsBias(location.latitude, location.longitude, showFeedback)
                } else {
                    requestSingleHighAccuracyLocation(showFeedback)
                }
            }
            .addOnFailureListener {
                binding.tvGpsStatus.text = "Could not read GPS. Search still works globally."
            }
    }

    private fun requestSingleHighAccuracyLocation(showFeedback: Boolean) {
        if (!hasLocationPermission()) return

        val tokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateGpsBias(location.latitude, location.longitude, showFeedback)
                } else {
                    binding.tvGpsStatus.text = "GPS unavailable right now. Move outdoors and retry."
                }
            }
            .addOnFailureListener {
                binding.tvGpsStatus.text = "GPS unavailable right now. Move outdoors and retry."
            }
    }

    private fun updateGpsBias(lat: Double, lng: Double, showFeedback: Boolean) {
        gpsBias = lat to lng
        binding.tvGpsStatus.text = "GPS ready: ${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
        if (showFeedback) {
            Toast.makeText(requireContext(), "Nearby place suggestions enabled", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Saved Locations ──

    private fun setupSavedLocations() {
        savedLocationsAdapter = FrequentLocationAdapter { location ->
            binding.etDestination.setText(location.name)
            selectedFromSearch = true
            walkSessionManager.setDestination(location.name, location.latitude, location.longitude)
            binding.rvPlacesSuggestions.visibility = View.GONE
            binding.cardSuggestions.visibility = View.GONE
        }
        binding.rvLocations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLocations.adapter = savedLocationsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            locationRepository.getLocations().collectLatest { locations ->
                if (_binding == null) return@collectLatest
                savedLocationsAdapter.submitList(locations)
                val empty = locations.isEmpty()
                binding.tvNoSaved.visibility = if (empty) View.VISIBLE else View.GONE
                binding.rvLocations.visibility = if (empty) View.GONE else View.VISIBLE
            }
        }
    }

    // ── Permissions & Start ──

    private fun checkPermissionsAndStart() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            startWalk()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun startWalk() {
        val intent = Intent(requireContext(), WalkForegroundService::class.java).apply {
            action = WalkForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        findNavController().navigate(R.id.action_walkDestination_to_activeWalk)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
