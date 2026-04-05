package com.example.way.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.way.R
import com.example.way.databinding.FragmentDashboardBinding
import com.example.way.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Dashboard — main screen with welcome message, walk analytics summary,
 * and the central START WALK button.
 *
 * TODO Phase 2: Inject DashboardViewModel for analytics from Room/Firestore.
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSettingsQuick.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // Time-based greeting
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> getString(R.string.dashboard_good_morning)
            hour < 17 -> getString(R.string.dashboard_good_afternoon)
            else -> getString(R.string.dashboard_good_evening)
        }
        binding.tvGreetingLabel.text = greeting

        // Welcome message with real user name
        val userName = authViewModel.getCurrentUserName()
        binding.tvWelcome.text = getString(R.string.dashboard_welcome, userName)

        // Observe analytics
        dashboardViewModel.totalWalks.observe(viewLifecycleOwner) { count ->
            binding.tvTotalWalks.text = count.toString()
        }
        dashboardViewModel.totalAlerts.observe(viewLifecycleOwner) { count ->
            binding.tvTotalAlerts.text = count.toString()
        }
        dashboardViewModel.lastSession.observe(viewLifecycleOwner) { session ->
            if (session != null) {
                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(session.date))
                val mins = session.durationSeconds / 60
                binding.tvLastWalkInfo.text = getString(R.string.dashboard_last_walk_summary, dateStr, mins)
            } else {
                binding.tvLastWalkInfo.text = getString(R.string.dashboard_no_walks)
            }
        }

        // START WALK button navigates to walk destination screen
        binding.btnStartWalk.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_walk)
        }

        // Pulse animation on the outer ring
        animateStartWalkButton()
    }

    private fun animateStartWalkButton() {
        if (_binding == null) return
        binding.startWalkOuterRing.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .alpha(0.2f)
            .setDuration(1200)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.startWalkOuterRing.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(0.4f)
                    .setDuration(1200)
                    .withEndAction {
                        if (isAdded && _binding != null) {
                            animateStartWalkButton()
                        }
                    }
                    .start()
            }
            .start()
    }

    override fun onDestroyView() {
        // Cancel any running animations to prevent callbacks after view destroy
        _binding?.startWalkOuterRing?.animate()?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
