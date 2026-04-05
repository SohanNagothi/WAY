package com.example.way.data.model

/**
 * Represents the walk session state machine.
 * Transitions: IDLE → WALKING → SAFETY_CHECK → EMERGENCY
 *
 * The walk can return from SAFETY_CHECK → WALKING (if user confirms safety),
 * or escalate from SAFETY_CHECK → EMERGENCY (if user fails to respond).
 * EMERGENCY → IDLE only after correct emergency code is entered.
 */
sealed class WalkState {
    /** No active walk session. Default state. */
    data object Idle : WalkState()

    /** Walk is in progress, sensors are monitoring. */
    data object Walking : WalkState()

    /** A safety trigger was detected, awaiting user confirmation. */
    data object SafetyCheck : WalkState()

    /** Emergency mode activated — alerts being sent. */
    data object Emergency : WalkState()
}

