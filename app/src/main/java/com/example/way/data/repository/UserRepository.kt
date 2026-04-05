package com.example.way.data.repository

import com.example.way.data.model.User
import com.example.way.util.Result

/**
 * Repository for user profile operations.
 * TODO Phase 1: Implement with Firebase Auth + Firestore.
 */
interface UserRepository {
    suspend fun getCurrentUser(): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}

