package com.example.way.data.model

/**
 * Represents a completed walk session stored in history.
 */
data class WalkSession(
    val id: String = "",
    val userId: String = "",
    val destinationName: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val distanceMeters: Float = 0f,
    val alertTriggered: Boolean = false,
    val triggerType: String? = null,
    val date: Long = System.currentTimeMillis()
)

