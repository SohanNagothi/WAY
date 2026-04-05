package com.example.way.ui.walk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.way.R
import com.example.way.data.local.PrefsManager
import com.example.way.data.model.WalkEvent
import com.example.way.data.model.WalkSession
import com.example.way.data.model.WalkState
import com.example.way.data.model.SafetyTrigger
import com.example.way.data.repository.WalkSessionRepository
import com.example.way.databinding.FragmentWalkBinding
import com.example.way.service.EmergencyHandler
import com.example.way.service.WalkForegroundService
import com.example.way.service.WalkSessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ActiveWalkFragment : Fragment() {

    private var _binding: FragmentWalkBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var walkSessionManager: WalkSessionManager
    @Inject lateinit var walkSessionRepository: WalkSessionRepository
    @Inject lateinit var emergencyHandler: EmergencyHandler
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var prefsManager: PrefsManager

    private var navigatedToSafetyCheck = false
    private var isEndingWalk = false
    private val gson = Gson()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDestination.text = walkSessionManager.destinationName
        navigatedToSafetyCheck = false

        // Back press confirmation
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showEndWalkConfirmation()
                }
            }
        )

        // SOS button
        binding.btnSOS.setOnClickListener {
            walkSessionManager.processEvent(
                WalkEvent.SafetyTriggered(SafetyTrigger.ManualSOS)
            )
        }

        // End Walk button
        binding.btnEndWalk.setOnClickListener {
            showEndWalkConfirmation()
        }

        observeState()
    }

    override fun onResume() {
        super.onResume()
        // Reset flag when coming back from SafetyCheck
        navigatedToSafetyCheck = false
    }

    private fun showEndWalkConfirmation() {
        if (_binding == null) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("End Walk?")
            .setMessage("Are you sure you want to stop monitoring?")
            .setPositiveButton("End Walk") { _, _ -> endWalk() }
            .setNegativeButton("Keep Walking", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                walkSessionManager.elapsedTimeSeconds.collectLatest { seconds ->
                    if (_binding == null) return@collectLatest
                    val mins = seconds / 60
                    val secs = seconds % 60
                    binding.tvElapsedTime.text = String.format(Locale.US, "%02d:%02d", mins, secs)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                walkSessionManager.distanceToDestination.collectLatest { meters ->
                    if (_binding == null) return@collectLatest

                    val hasDestinationCoords =
                        walkSessionManager.destinationLat != 0.0 && walkSessionManager.destinationLng != 0.0

                    val displayMeters = if (hasDestinationCoords) {
                        meters
                    } else {
                        walkSessionManager.totalDistanceMeters.value
                    }

                    val display = if (displayMeters >= 1000) {
                        String.format(Locale.US, "%.1f km", displayMeters / 1000)
                    } else {
                        String.format(Locale.US, "%.0f m", displayMeters)
                    }
                    binding.tvDistance.text = display
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                walkSessionManager.walkState.collectLatest { state ->
                    if (_binding == null) return@collectLatest
                    updateStateUI(state)

                    when (state) {
                        is WalkState.SafetyCheck -> {
                            if (!navigatedToSafetyCheck) {
                                navigatedToSafetyCheck = true
                                try {
                                    findNavController().navigate(R.id.action_activeWalk_to_safetyCheck)
                                } catch (e: Exception) {
                                    Log.e("ActiveWalk", "Navigation error", e)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateStateUI(state: WalkState) {
        when (state) {
            is WalkState.Walking -> {
                binding.chipWalkState.text = getString(R.string.walk_state_walking)
                binding.chipWalkState.setChipBackgroundColorResource(R.color.way_primary_container)
                binding.chipWalkState.setTextColor(ContextCompat.getColor(requireContext(), R.color.way_primary))
                binding.btnSOS.isEnabled = true
            }
            is WalkState.SafetyCheck -> {
                binding.chipWalkState.text = getString(R.string.walk_state_safety_check)
                binding.chipWalkState.setChipBackgroundColorResource(R.color.way_card_tint_amber)
                binding.chipWalkState.setTextColor(ContextCompat.getColor(requireContext(), R.color.way_warning_amber))
            }
            is WalkState.Emergency -> {
                binding.chipWalkState.text = getString(R.string.walk_state_emergency)
                binding.chipWalkState.setChipBackgroundColorResource(R.color.way_error_container)
                binding.chipWalkState.setTextColor(ContextCompat.getColor(requireContext(), R.color.way_error))
            }
            is WalkState.Idle -> {
                binding.chipWalkState.text = getString(R.string.walk_state_idle)
            }
        }
    }

    private fun buildCurrentSession(): WalkSession {
        val now = System.currentTimeMillis()
        val uid = auth.currentUser?.uid ?: prefsManager.userUid
        val stableSessionId = "${uid}_${walkSessionManager.startTime}"
        return WalkSession(
            id = stableSessionId,
            userId = uid,
            destinationName = walkSessionManager.destinationName,
            destinationLat = walkSessionManager.destinationLat,
            destinationLng = walkSessionManager.destinationLng,
            startTime = walkSessionManager.startTime,
            endTime = now,
            durationSeconds = walkSessionManager.elapsedTimeSeconds.value,
            distanceMeters = walkSessionManager.totalDistanceMeters.value,
            alertTriggered = walkSessionManager.alertTriggered,
            triggerType = walkSessionManager.triggerType,
            date = walkSessionManager.startTime
        )
    }

    private fun endWalk() {
        if (isEndingWalk) return
        isEndingWalk = true

        val session = buildCurrentSession()

        // Local fail-safe: always cache locally first
        try {
            prefsManager.cachedWalkSessionJson = gson.toJson(session)
        } catch (_: Exception) {}

        viewLifecycleOwner.lifecycleScope.launch {
            val result = walkSessionRepository.saveSession(session)
            if (result is com.example.way.util.Result.Success) {
                prefsManager.clearCachedWalkSession()
            }
        }

        walkSessionManager.processEvent(WalkEvent.EndWalk)
        walkSessionManager.reset()
        emergencyHandler.clearEmergencyNotification()

        // Stop service
        val intent = Intent(requireContext(), WalkForegroundService::class.java).apply {
            action = WalkForegroundService.ACTION_STOP
        }
        requireContext().startService(intent)

        try {
            findNavController().popBackStack(R.id.dashboardFragment, false)
        } catch (e: Exception) {
            Log.e("ActiveWalk", "PopBackStack error", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
