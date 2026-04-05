package com.example.way.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.way.data.local.PrefsManager
import com.example.way.data.model.Contact
import com.example.way.data.repository.ContactsRepository
import com.example.way.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Shared ViewModel for setup wizard steps.
 * Holds transient state across the 4 setup pages.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    private val contactsRepository: ContactsRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Emergency contacts collected during setup (name, phone pairs)
    data class SetupContact(val name: String, val phone: String)

    private val _contacts = MutableLiveData<List<SetupContact>>(emptyList())
    val contacts: LiveData<List<SetupContact>> = _contacts

    private val _emergencyCode = MutableLiveData<String>()
    val emergencyCode: LiveData<String> = _emergencyCode

    private val _permissionsGranted = MutableLiveData(false)
    val permissionsGranted: LiveData<Boolean> = _permissionsGranted

    fun setContacts(contacts: List<SetupContact>) {
        _contacts.value = contacts
    }

    fun setEmergencyCode(code: String) {
        _emergencyCode.value = code
    }

    fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    /**
     * Persists setup data and marks setup as complete.
     * Saves emergency contacts to Firestore and emergency code locally.
     */
    fun completeSetup() {
        // Save emergency code locally
        _emergencyCode.value?.let { prefsManager.emergencyCode = it }

        // Save contacts to Firestore
        val setupContacts = _contacts.value ?: emptyList()
        viewModelScope.launch {
            setupContacts.forEachIndexed { index, setupContact ->
                val contact = Contact(
                    name = setupContact.name,
                    phone = setupContact.phone,
                    priority = index
                )
                contactsRepository.addContact(contact)
            }
        }

        // Mark setup as complete locally
        prefsManager.isSetupComplete = true

        // Persist setup flag in Firestore for future logins on any device
        val uid = auth.currentUser?.uid ?: prefsManager.userUid
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    firestore.collection(Constants.COLLECTION_USERS)
                        .document(uid)
                        .set(mapOf("setupComplete" to true), SetOptions.merge())
                        .await()
                } catch (_: Exception) {
                    // Non-fatal: local flag already marked true
                }
            }
        }
    }
}
