package com.example.way.data.model

/**
 * Types of safety triggers that can occur during a walk.
 */
sealed class SafetyTrigger {
    /** User has been stationary for too long. */
    data class Inactivity(val durationSeconds: Long) : SafetyTrigger()

    /** Accelerometer detected a phone fall/drop. */
    data object PhoneFall : SafetyTrigger()

    /** User's speed exceeded the walking threshold. */
    data class SuddenSpeed(val speedMps: Float) : SafetyTrigger()

    /** Distance to destination has been increasing continuously. */
    data class DistanceIncreasing(val currentDistanceMeters: Float) : SafetyTrigger()

    /** User manually pressed the SOS button. */
    data object ManualSOS : SafetyTrigger()
}

