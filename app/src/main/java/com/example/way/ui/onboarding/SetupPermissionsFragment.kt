package com.example.way.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.way.R
import com.example.way.databinding.FragmentSetupPermissionsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Setup Step 4: Request necessary permissions.
 */
@AndroidEntryPoint
class SetupPermissionsFragment : Fragment() {

    private var _binding: FragmentSetupPermissionsBinding? = null
    private val binding get() = _binding!!
    private val setupViewModel: SetupViewModel by activityViewModels()

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        updatePermissionStates()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePermissionStates()

        binding.cardLocationPerm.setOnClickListener { requestLocationPermission() }
        binding.cardActivityPerm.setOnClickListener { requestActivityPermission() }
        binding.cardNotifPerm.setOnClickListener { requestNotificationPermission() }
        binding.btnGrantAll.setOnClickListener { requestAllPermissions() }
    }

    private fun requestAllPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    private fun requestLocationPermission() {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private fun updatePermissionStates() {
        val ctx = context ?: return

        val locationGranted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        setStatusIcon(binding.ivLocationStatus, locationGranted)
        setStatusIcon(binding.ivActivityStatus, activityGranted)
        setStatusIcon(binding.ivNotifStatus, notifGranted)

        val allGranted = locationGranted && activityGranted && notifGranted
        setupViewModel.setPermissionsGranted(allGranted)
        binding.btnGrantAll.isEnabled = !allGranted
        if (allGranted) {
            binding.btnGrantAll.text = getString(R.string.setup_perm_granted)
        }
    }

    private fun setStatusIcon(imageView: android.widget.ImageView, granted: Boolean) {
        if (granted) {
            imageView.setImageResource(R.drawable.ic_check)
            imageView.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.way_safe_green)
            )
        } else {
            imageView.setImageResource(R.drawable.ic_chevron_right)
            imageView.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.way_outline)
            )
        }
    }

    /** Permissions step is always valid — user can proceed without granting. */
    fun validate(): Boolean = true

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

