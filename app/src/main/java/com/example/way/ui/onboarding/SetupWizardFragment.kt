package com.example.way.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.way.R
import com.example.way.databinding.FragmentSetupWizardBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Setup wizard — guides first-time users through 4 steps:
 *   Step 1: Add emergency contacts
 *   Step 2: Add frequent locations (skippable)
 *   Step 3: Set emergency code (4-digit PIN)
 *   Step 4: Grant permissions
 */
@AndroidEntryPoint
class SetupWizardFragment : Fragment() {

    private var _binding: FragmentSetupWizardBinding? = null
    private val binding get() = _binding!!
    private val setupViewModel: SetupViewModel by activityViewModels()
    private lateinit var adapter: SetupPagerAdapter
    private val stepViews get() = listOf(binding.step1, binding.step2, binding.step3, binding.step4)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupWizardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SetupPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // navigate via buttons only

        updateStepIndicator(0)
        updateButtons(0)

        binding.btnNext.setOnClickListener { onNextClicked() }
        binding.btnBack.setOnClickListener { onBackClicked() }
    }

    private fun onNextClicked() {
        val currentPage = binding.viewPager.currentItem

        // Validate current step
        if (!validateCurrentStep(currentPage)) return

        if (currentPage < adapter.itemCount - 1) {
            // Go to next step
            binding.viewPager.setCurrentItem(currentPage + 1, true)
            updateStepIndicator(currentPage + 1)
            updateButtons(currentPage + 1)
        } else {
            // Last step — complete setup
            setupViewModel.completeSetup()
            findNavController().navigate(R.id.action_setup_to_dashboard)
        }
    }

    private fun onBackClicked() {
        val currentPage = binding.viewPager.currentItem
        if (currentPage > 0) {
            binding.viewPager.setCurrentItem(currentPage - 1, true)
            updateStepIndicator(currentPage - 1)
            updateButtons(currentPage - 1)
        }
    }

    private fun validateCurrentStep(position: Int): Boolean {
        val fragment = childFragmentManager.findFragmentByTag("f$position")
        return when (fragment) {
            is SetupContactsFragment -> fragment.validate()
            is SetupLocationsFragment -> fragment.validate()
            is SetupCodeFragment -> fragment.validate()
            is SetupPermissionsFragment -> fragment.validate()
            else -> true
        }
    }

    private fun updateStepIndicator(currentPage: Int) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.way_primary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.way_outline_variant)

        stepViews.forEachIndexed { index, view ->
            if (index <= currentPage) {
                view.setBackgroundColor(activeColor)
            } else {
                view.setBackgroundColor(inactiveColor)
            }
        }
    }

    private fun updateButtons(currentPage: Int) {
        binding.btnBack.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        binding.btnNext.text = if (currentPage == adapter.itemCount - 1) {
            getString(R.string.btn_finish)
        } else {
            getString(R.string.btn_next)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
