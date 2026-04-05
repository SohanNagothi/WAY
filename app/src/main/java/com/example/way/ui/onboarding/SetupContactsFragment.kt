package com.example.way.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.way.databinding.FragmentSetupContactsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Setup Step 1: Add emergency contacts.
 */
@AndroidEntryPoint
class SetupContactsFragment : Fragment() {

    private var _binding: FragmentSetupContactsBinding? = null
    private val binding get() = _binding!!
    private val setupViewModel: SetupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveContacts()
        _binding = null
    }

    /**
     * Validates and returns true if at least one contact is filled.
     */
    fun validate(): Boolean {
        val name1 = binding.etContactName1.text.toString().trim()
        val phone1 = binding.etContactPhone1.text.toString().trim()

        if (name1.isEmpty()) {
            binding.tilContactName1.error = "At least one contact is required"
            return false
        } else {
            binding.tilContactName1.error = null
        }

        if (phone1.isEmpty()) {
            binding.tilContactPhone1.error = "Phone number is required"
            return false
        } else {
            binding.tilContactPhone1.error = null
        }

        saveContacts()
        return true
    }

    private fun saveContacts() {
        val b = _binding ?: return
        val contacts = mutableListOf<SetupViewModel.SetupContact>()

        val name1 = b.etContactName1.text.toString().trim()
        val phone1 = b.etContactPhone1.text.toString().trim()
        if (name1.isNotEmpty() && phone1.isNotEmpty()) {
            contacts.add(SetupViewModel.SetupContact(name1, phone1))
        }

        val name2 = b.etContactName2.text.toString().trim()
        val phone2 = b.etContactPhone2.text.toString().trim()
        if (name2.isNotEmpty() && phone2.isNotEmpty()) {
            contacts.add(SetupViewModel.SetupContact(name2, phone2))
        }

        setupViewModel.setContacts(contacts)
    }
}

