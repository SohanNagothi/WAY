package com.example.way.data.model

/**
 * Represents a user in the system.
 * Used for both local and Firestore storage.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val emergencyCode: String = "",
    val setupComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

