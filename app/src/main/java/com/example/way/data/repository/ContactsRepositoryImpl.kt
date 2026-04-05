package com.example.way.data.repository

import com.example.way.data.local.PrefsManager
import com.example.way.data.model.Contact
import com.example.way.util.Constants
import com.example.way.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefsManager: PrefsManager
) : ContactsRepository {

    private fun contactsCollection() =
        firestore.collection(Constants.COLLECTION_USERS)
            .document(auth.currentUser?.uid ?: prefsManager.userUid)
            .collection(Constants.COLLECTION_CONTACTS)

    override fun getContacts(): Flow<List<Contact>> = callbackFlow {
        val listener = contactsCollection()
            .orderBy("priority", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fall back to empty list on error
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val contacts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Contact::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(contacts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addContact(contact: Contact): Result<Unit> {
        return try {
            val docRef = contactsCollection().document()
            val contactWithId = contact.copy(id = docRef.id)
            withTimeoutOrNull(8000L) {
                docRef.set(contactWithId).await()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to add contact", e)
        }
    }

    override suspend fun updateContact(contact: Contact): Result<Unit> {
        return try {
            withTimeoutOrNull(8000L) {
                contactsCollection().document(contact.id).set(contact).await()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to update contact", e)
        }
    }

    override suspend fun deleteContact(contactId: String): Result<Unit> {
        return try {
            withTimeoutOrNull(8000L) {
                contactsCollection().document(contactId).delete().await()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to delete contact", e)
        }
    }

    override suspend fun getHighestPriorityContact(): Result<Contact> {
        return try {
            val snapshot = contactsCollection()
                .orderBy("priority", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .await()
            val contact = snapshot.documents.firstOrNull()?.toObject(Contact::class.java)
                ?: return Result.Error("No emergency contacts found")
            Result.Success(contact)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to get contact", e)
        }
    }
}

