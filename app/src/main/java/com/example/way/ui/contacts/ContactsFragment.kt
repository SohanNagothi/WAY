package com.example.way.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.way.R
import com.example.way.data.model.Contact
import com.example.way.databinding.DialogAddEditContactBinding
import com.example.way.databinding.FragmentContactsBinding
import com.example.way.util.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Emergency Contacts screen — CRUD operations for emergency contacts.
 * Shows a RecyclerView list with FAB to add new contacts.
 *
 * TODO Phase 2: Inject ContactsViewModel, set up RecyclerView adapter,
 *              implement add/edit dialog and swipe-to-delete.
 */
@AndroidEntryPoint
class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabAddContact.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter(
            onEdit = { contact -> showAddEditDialog(contact) },
            onDelete = { contact -> showDeleteConfirmation(contact) },
            onSetPrimary = { contact -> viewModel.setPrimaryContact(contact) }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter
    }

    private fun observeData() {
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submitList(contacts)
            val isEmpty = contacts.isEmpty()
            binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvContacts.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            if (result is Result.Error) {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddEditDialog(contact: Contact?) {
        val dialogBinding = DialogAddEditContactBinding.inflate(layoutInflater)
        val isEdit = contact != null

        dialogBinding.tvDialogTitle.text = if (isEdit) "Edit Contact" else "Add Contact"
        contact?.let {
            dialogBinding.etName.setText(it.name)
            dialogBinding.etPhone.setText(it.phone)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val name = dialogBinding.etName.text.toString().trim()
                val phone = dialogBinding.etPhone.text.toString().trim()

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isEdit) {
                    viewModel.updateContact(contact!!.copy(name = name, phone = phone))
                } else {
                    viewModel.addContact(name, phone)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(contact: Contact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Remove ${contact.name} from your emergency contacts?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteContact(contact.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
