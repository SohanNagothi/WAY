package com.example.way.ui.walk

import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.way.data.local.PrefsManager
import com.example.way.data.model.WalkEvent
import com.example.way.databinding.FragmentSafetyCheckBinding
import com.example.way.service.WalkSessionManager
import com.example.way.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SafetyCheckFragment : Fragment() {

    private var _binding: FragmentSafetyCheckBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var walkSessionManager: WalkSessionManager
    @Inject lateinit var prefsManager: PrefsManager

    private var countdownTimer: CountDownTimer? = null
    private var attemptsRemaining = Constants.MAX_CODE_ATTEMPTS
    private var resolved = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSafetyCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resolved = false
        startVibration()
        startCountdown()

        binding.btnImOkay.setOnClickListener {
            countdownTimer?.cancel()
            showCodeEntry()
        }

        binding.btnConfirmCode.setOnClickListener {
            validateCode()
        }
    }

    private fun startCountdown() {
        countdownTimer = object : CountDownTimer(
            Constants.SAFETY_CHECK_COUNTDOWN_SECONDS * 1000L, 1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.tvCountdown.text = seconds.toString()
            }

            override fun onFinish() {
                safetyFailed()
            }
        }.start()
    }

    private fun showCodeEntry() {
        if (_binding == null) return
        binding.btnImOkay.visibility = View.GONE
        binding.tilCode.visibility = View.VISIBLE
        binding.btnConfirmCode.visibility = View.VISIBLE
        binding.etCode.requestFocus()
    }

    private fun validateCode() {
        if (_binding == null || resolved) return
        val enteredCode = binding.etCode.text.toString().trim()
        val correctCode = prefsManager.emergencyCode

        if (enteredCode == correctCode) {
            resolved = true
            stopVibration()
            countdownTimer?.cancel()
            walkSessionManager.processEvent(WalkEvent.SafetyConfirmed)
            safeNavigateBack()
        } else {
            attemptsRemaining--
            if (attemptsRemaining > 0) {
                binding.tvAttempts.visibility = View.VISIBLE
                binding.tvAttempts.text = "Wrong code. $attemptsRemaining attempt(s) remaining."
                binding.etCode.text?.clear()
            } else {
                safetyFailed()
            }
        }
    }

    private fun safetyFailed() {
        if (resolved) return
        resolved = true
        stopVibration()
        countdownTimer?.cancel()
        walkSessionManager.processEvent(WalkEvent.SafetyFailed)
        safeNavigateBack()
    }

    private fun safeNavigateBack() {
        try {
            if (isAdded && _binding != null) {
                findNavController().popBackStack()
            }
        } catch (e: Exception) {
            Log.e("SafetyCheck", "Navigation error", e)
        }
    }

    private fun startVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService<VibratorManager>()
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), 0)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = requireContext().getSystemService<Vibrator>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), 0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 300, 500), 0)
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requireContext().getSystemService<VibratorManager>()?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService<Vibrator>()?.cancel()
            }
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        stopVibration()
        _binding = null
    }
}
