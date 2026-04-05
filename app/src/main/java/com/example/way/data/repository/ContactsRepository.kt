package com.example.way.data.repository

import com.example.way.data.model.Contact
import com.example.way.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for emergency contact operations.
 * TODO Phase 2: Implement with Room (local) + Firestore (remote) dual-write.
 */
interface ContactsRepository {
    fun getContacts(): Flow<List<Contact>>
    suspend fun addContact(contact: Contact): Result<Unit>
    suspend fun updateContact(contact: Contact): Result<Unit>
    suspend fun deleteContact(contactId: String): Result<Unit>
    suspend fun getHighestPriorityContact(): Result<Contact>
}

