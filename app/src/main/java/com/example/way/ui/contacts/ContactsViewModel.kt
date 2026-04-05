package com.example.way.ui.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.way.data.model.Contact
import com.example.way.data.repository.ContactsRepository
import com.example.way.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = _contacts

    private val _operationResult = MutableLiveData<Result<Unit>>()
    val operationResult: LiveData<Result<Unit>> = _operationResult

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            contactsRepository.getContacts().collectLatest { list ->
                _contacts.value = list
            }
        }
    }

    fun addContact(name: String, phone: String) {
        val currentList = _contacts.value ?: emptyList()
        val contact = Contact(
            name = name,
            phone = phone,
            priority = currentList.size // next priority
        )
        viewModelScope.launch {
            _operationResult.value = contactsRepository.addContact(contact)
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            _operationResult.value = contactsRepository.updateContact(contact)
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _operationResult.value = contactsRepository.deleteContact(contactId)
        }
    }

    fun setPrimaryContact(contact: Contact) {
        viewModelScope.launch {
            val currentList = _contacts.value ?: return@launch
            // Move selected to priority 0, shift others
            currentList.forEachIndexed { index, c ->
                val newPriority = when {
                    c.id == contact.id -> 0
                    c.priority < contact.priority -> c.priority + 1
                    else -> c.priority
                }
                if (newPriority != c.priority) {
                    contactsRepository.updateContact(c.copy(priority = newPriority))
                }
            }
            contactsRepository.updateContact(contact.copy(priority = 0))
        }
    }
}

