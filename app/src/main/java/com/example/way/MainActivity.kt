package com.example.way

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.way.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Destinations where the bottom navigation bar should be visible
    private val bottomNavDestinations = setOf(
        R.id.dashboardFragment,
        R.id.contactsFragment,
        R.id.historyFragment,
        R.id.selfDefenseFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Set up Toolbar as ActionBar
        setSupportActionBar(binding.toolbar)
        binding.toolbar.menu.clear()
        binding.toolbar.inflateMenu(R.menu.top_bar_menu)

        // Set up Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Top-level destinations (no back arrow shown for these)
        val appBarConfiguration = AppBarConfiguration(bottomNavDestinations)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        // Show/hide bottom nav and toolbar based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isMainScreen = destination.id in bottomNavDestinations
            binding.bottomNav.visibility = if (isMainScreen) View.VISIBLE else View.GONE

            // Hide toolbar on splash, auth, and immersive walk screens
            val hideToolbar = destination.id in setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.signupFragment,
                R.id.setupWizardFragment,
                R.id.safetyCheckFragment
            )
            binding.toolbar.visibility = if (hideToolbar) View.GONE else View.VISIBLE

            binding.toolbar.menu.findItem(R.id.action_settings)?.isVisible =
                !hideToolbar && destination.id != R.id.settingsFragment
        }

        // Settings button in toolbar
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}