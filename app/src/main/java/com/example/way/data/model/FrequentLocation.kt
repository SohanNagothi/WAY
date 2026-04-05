package com.example.way.data.model

/**
 * Represents a saved/frequent location (home, work, gym, etc.).
 */
data class FrequentLocation(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

