package com.example.way.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.way.BuildConfig
import com.example.way.R
import com.example.way.data.local.PrefsManager
import com.example.way.databinding.FragmentSettingsBinding
import com.example.way.ui.auth.AuthViewModel
import com.example.way.util.BatteryOptimizationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Settings screen — profile editing, emergency code, frequent locations,
 * battery optimization, theme switching, and sign out.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    @Inject lateinit var prefsManager: PrefsManager

    private val themeOptions = arrayOf("System default", "Light", "Dark")
    private val themeModes = intArrayOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateThemeLabel()
        binding.tvAppVersion.text = "Version ${BuildConfig.VERSION_NAME}"

        binding.cardProfile.setOnClickListener {
            findNavController().navigate(R.id.contactsFragment)
        }

        binding.cardEmergencyCode.setOnClickListener {
            showChangeEmergencyCodeDialog()
        }

        binding.cardLocations.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_locations)
        }

        binding.cardBattery.setOnClickListener {
            BatteryOptimizationHelper.requestDisableBatteryOptimization(requireContext())
        }

        binding.cardTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.btnSignOut.setOnClickListener {
            showSignOutConfirmation()
        }
    }

    private fun showChangeEmergencyCodeDialog() {
        val inputLayout = TextInputLayout(requireContext()).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(48, 32, 48, 0)
            hint = "New Emergency Code"
        }
        val editText = TextInputEditText(inputLayout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Emergency Code")
            .setMessage("Enter a new numeric code (min 4 digits)")
            .setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                val code = editText.text.toString().trim()
                if (code.length >= 4) {
                    prefsManager.emergencyCode = code
                    Toast.makeText(requireContext(), "Emergency code updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Code must be at least 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSignOutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                prefsManager.clear()
                authViewModel.signOut()
                findNavController().navigate(R.id.action_global_login)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemeDialog() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val checkedItem = themeModes.indexOf(currentMode).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose theme")
            .setSingleChoiceItems(themeOptions, checkedItem) { dialog, which ->
                AppCompatDelegate.setDefaultNightMode(themeModes[which])
                prefsManager.themeMode = themeModes[which]
                updateThemeLabel()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateThemeLabel() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val index = themeModes.indexOf(currentMode).coerceAtLeast(0)
        binding.tvThemeValue.text = themeOptions[index]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
