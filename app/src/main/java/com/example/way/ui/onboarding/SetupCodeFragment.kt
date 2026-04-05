package com.example.way.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.way.databinding.FragmentSetupCodeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Setup Step 3: Set 4-digit emergency code.
 */
@AndroidEntryPoint
class SetupCodeFragment : Fragment() {

    private var _binding: FragmentSetupCodeBinding? = null
    private val binding get() = _binding!!
    private val setupViewModel: SetupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun validate(): Boolean {
        val code = binding.etCode.text.toString()
        val confirm = binding.etConfirmCode.text.toString()

        if (code.length != 4) {
            binding.tilCode.error = "Enter a 4-digit PIN"
            return false
        } else {
            binding.tilCode.error = null
        }

        if (confirm != code) {
            binding.tilConfirmCode.error = "PINs don't match"
            return false
        } else {
            binding.tilConfirmCode.error = null
        }

        setupViewModel.setEmergencyCode(code)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

