package com.example.way.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.way.R
import com.example.way.databinding.FragmentSplashBinding
import com.example.way.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playEntranceAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAdded || _binding == null) return@postDelayed

            try {
                when {
                    authViewModel.isLoggedIn() -> {
                        findNavController().navigate(R.id.action_splash_to_dashboard)
                    }
                    else -> {
                        findNavController().navigate(R.id.action_splash_to_login)
                    }
                }
            } catch (e: Exception) {
                Log.e("SplashFragment", "Navigation failed", e)
            }
        }, 2500)
    }

    private fun playEntranceAnimation() {
        // Initially hide elements
        binding.ivLogo.alpha = 0f
        binding.ivLogo.scaleX = 0.3f
        binding.ivLogo.scaleY = 0.3f
        binding.tvAppName.alpha = 0f
        binding.tvAppName.translationY = 30f
        binding.tvTagline.alpha = 0f
        binding.tvTagline.translationY = 20f
        binding.progressBar.alpha = 0f

        // Logo: scale up + fade in with overshoot
        val logoScaleX = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_X, 0.3f, 1f).apply {
            interpolator = OvershootInterpolator(1.5f)
        }
        val logoScaleY = ObjectAnimator.ofFloat(binding.ivLogo, View.SCALE_Y, 0.3f, 1f).apply {
            interpolator = OvershootInterpolator(1.5f)
        }
        val logoAlpha = ObjectAnimator.ofFloat(binding.ivLogo, View.ALPHA, 0f, 1f)

        val logoSet = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoAlpha)
            duration = 700
        }

        // App name: fade in + slide up
        val nameAlpha = ObjectAnimator.ofFloat(binding.tvAppName, View.ALPHA, 0f, 1f)
        val nameTransY = ObjectAnimator.ofFloat(binding.tvAppName, View.TRANSLATION_Y, 30f, 0f)
        val nameSet = AnimatorSet().apply {
            playTogether(nameAlpha, nameTransY)
            duration = 500
            startDelay = 400
        }

        // Tagline: fade in + slide up
        val taglineAlpha = ObjectAnimator.ofFloat(binding.tvTagline, View.ALPHA, 0f, 1f)
        val taglineTransY = ObjectAnimator.ofFloat(binding.tvTagline, View.TRANSLATION_Y, 20f, 0f)
        val taglineSet = AnimatorSet().apply {
            playTogether(taglineAlpha, taglineTransY)
            duration = 400
            startDelay = 700
        }

        // Progress: fade in
        val progressAlpha = ObjectAnimator.ofFloat(binding.progressBar, View.ALPHA, 0f, 1f).apply {
            duration = 400
            startDelay = 1000
        }

        AnimatorSet().apply {
            playTogether(logoSet, nameSet, taglineSet, progressAlpha)
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
