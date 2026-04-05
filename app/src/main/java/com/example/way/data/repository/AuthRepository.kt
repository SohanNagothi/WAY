package com.example.way.data.repository

import com.example.way.data.model.User
import com.example.way.util.Result

/**
 * Repository for authentication operations.
 * Abstracts Firebase Auth so ViewModels remain testable.
 */
interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    fun signOut()
    fun getCurrentUser(): User?
    fun isLoggedIn(): Boolean
}

