package com.example.way.data.model

/**
 * Represents an emergency contact.
 * Lower priority number = higher importance (1 = most important, called first).
 */
data class Contact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val priority: Int = 0
)

