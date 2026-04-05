package com.example.way.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.way.R
import com.example.way.data.model.FrequentLocation
import com.example.way.data.remote.GeoapifyPlace
import com.example.way.data.remote.GeoapifyService
import com.example.way.data.repository.LocationRepository
import com.example.way.databinding.FragmentSetupLocationsBinding
import com.example.way.ui.settings.FrequentLocationsAdapter
import com.example.way.util.Result
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Setup Step 2: Add frequent locations during onboarding.
 * Users can add locations with GPS capture or skip this step.
 */
@AndroidEntryPoint
class SetupLocationsFragment : Fragment() {

    private var _binding: FragmentSetupLocationsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var locationRepository: LocationRepository

    private lateinit var adapter: FrequentLocationsAdapter
    private var searchJob: Job? = null

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            withBestAvailableGps { gps ->
                showAddLocationDialog(gps)
            }
        } else {
            showAddLocationDialog(null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FrequentLocationsAdapter(
            onEdit = { /* Edit disabled in setup */ },
            onDelete = { deleteLocation(it) }
        )
        binding.rvLocations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLocations.adapter = adapter

        lifecycleScope.launch {
            locationRepository.getLocations().collectLatest { locations ->
                if (_binding == null) return@collectLatest
                adapter.submitList(locations)
                val empty = locations.isEmpty()
                binding.tvHint.visibility = if (empty) View.VISIBLE else View.GONE
                binding.rvLocations.visibility = if (empty) View.GONE else View.VISIBLE
            }
        }

        binding.fabAdd.setOnClickListener {
            requestLocationAndShowAddDialog()
        }
    }

    private fun requestLocationAndShowAddDialog() {
        if (hasLocationPermission()) {
            withBestAvailableGps { gps ->
                showAddLocationDialog(gps)
            }
        } else {
            showAddLocationDialog(null)
        }
    }

    private fun showAddLocationDialog(currentGps: Pair<Double, Double>? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_edit_location, null)
        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilName)
        val tilAddress = dialogView.findViewById<TextInputLayout>(R.id.tilAddress)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etAddress = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.etAddress)

        var selectedPlace: GeoapifyPlace? = null
        bindDialogAutocomplete(etAddress, currentGps) { place ->
            selectedPlace = place
            if (etName.text.isNullOrBlank()) {
                etName.setText(place.name)
            }
            etAddress.setText(place.fullAddress.ifBlank { place.name }, false)
            tilAddress.error = null
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Location")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = etName.text?.toString()?.trim().orEmpty()
                val address = etAddress.text?.toString()?.trim().orEmpty()

                tilName.error = null
                tilAddress.error = null

                var valid = true
                if (name.isBlank()) {
                    tilName.error = "Location name is required"
                    valid = false
                }
                if (address.isBlank()) {
                    tilAddress.error = "Address is required"
                    valid = false
                }
                if (!valid) return@setOnClickListener

                addLocationWithGps(name, address, selectedPlace?.let { it.latitude to it.longitude } ?: currentGps)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun bindDialogAutocomplete(
        etAddress: MaterialAutoCompleteTextView,
        gps: Pair<Double, Double>?,
        onPlaceSelected: (GeoapifyPlace) -> Unit
    ) {
        if (!GeoapifyService.isConfigured) return

        etAddress.threshold = 1

        val labels = mutableListOf<String>()
        val dropdownAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            labels
        )
        etAddress.setAdapter(dropdownAdapter)

        var suggestions: List<GeoapifyPlace> = emptyList()
        var suppressSearch = false

        etAddress.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && labels.isNotEmpty()) {
                etAddress.post { etAddress.showDropDown() }
            }
        }

        etAddress.setOnItemClickListener { _, _, position, _ ->
            val selected = suggestions.getOrNull(position) ?: return@setOnItemClickListener
            suppressSearch = true
            etAddress.setText(selected.fullAddress.ifBlank { selected.name }, false)
            suppressSearch = false
            onPlaceSelected(selected)
        }

        etAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (suppressSearch) return
                val query = s?.toString()?.trim().orEmpty()
                if (query.length < 3) {
                    labels.clear()
                    dropdownAdapter.notifyDataSetChanged()
                    return
                }

                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    var results = GeoapifyService.autocomplete(query, gps?.first, gps?.second)
                    if (results.isEmpty()) {
                        results = GeoapifyService.autocomplete(query)
                    }
                    suggestions = results
                    labels.clear()
                    labels.addAll(results.map { it.fullAddress.ifBlank { it.name } })
                    dropdownAdapter.notifyDataSetChanged()
                    if (labels.isNotEmpty()) {
                        etAddress.post { etAddress.showDropDown() }
                    }
                }
            }
        })
    }

    private fun addLocationWithGps(name: String, address: String, gps: Pair<Double, Double>?) {
        lifecycleScope.launch {
            val (lat, lng) = gps ?: (0.0 to 0.0)
            val location = FrequentLocation(
                name = name,
                address = address,
                latitude = lat,
                longitude = lng
            )
            when (val result = locationRepository.addLocation(location)) {
                is Result.Success -> Toast.makeText(requireContext(), "Location added", Toast.LENGTH_SHORT).show()
                is Result.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                Result.Loading -> Unit
            }
        }
    }

    private fun deleteLocation(location: FrequentLocation) {
        lifecycleScope.launch {
            when (val result = locationRepository.deleteLocation(location.id)) {
                is Result.Success -> Toast.makeText(requireContext(), "Location deleted", Toast.LENGTH_SHORT).show()
                is Result.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                Result.Loading -> Unit
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("MissingPermission")
    private fun withBestAvailableGps(onReady: (Pair<Double, Double>?) -> Unit) {
        if (!hasLocationPermission()) {
            onReady(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onReady(location.latitude to location.longitude)
                } else {
                    val tokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        tokenSource.token
                    ).addOnSuccessListener { current ->
                        onReady(current?.let { it.latitude to it.longitude })
                    }.addOnFailureListener {
                        onReady(null)
                    }
                }
            }
            .addOnFailureListener {
                onReady(null)
            }
    }

    /** Setup step remains skippable, but each added location enforces required fields. */
    fun validate(): Boolean = true

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
