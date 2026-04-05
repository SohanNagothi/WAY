package com.example.way.data.model

/**
 * Events that cause transitions in the WalkState machine.
 */
sealed class WalkEvent {
    /** User starts a walk session. */
    data object StartWalk : WalkEvent()

    /** A safety trigger was detected (inactivity, fall, speed, distance, SOS). */
    data class SafetyTriggered(val trigger: SafetyTrigger) : WalkEvent()

    /** User confirmed safety with correct code. */
    data object SafetyConfirmed : WalkEvent()

    /** Safety check failed (wrong code or timeout). */
    data object SafetyFailed : WalkEvent()

    /** Emergency resolved with correct code. */
    data object EmergencyResolved : WalkEvent()

    /** User manually ends the walk. */
    data object EndWalk : WalkEvent()
}

