package com.example.way.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager2 adapter for the 4 setup wizard steps.
 */
class SetupPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SetupContactsFragment()
        1 -> SetupLocationsFragment()
        2 -> SetupCodeFragment()
        3 -> SetupPermissionsFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}

