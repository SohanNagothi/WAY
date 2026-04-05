package com.example.way.service

import com.example.way.data.model.WalkEvent
import com.example.way.data.model.WalkState

/**
 * Enforces valid state transitions for the walk session.
 *
 * Valid transitions:
 *   IDLE ──(StartWalk)──> WALKING
 *   WALKING ──(SafetyTriggered)──> SAFETY_CHECK
 *   WALKING ──(EndWalk)──> IDLE
 *   SAFETY_CHECK ──(SafetyConfirmed)──> WALKING
 *   SAFETY_CHECK ──(SafetyFailed)──> EMERGENCY
 *   EMERGENCY ──(EmergencyResolved)──> IDLE
 *
 * Invalid transitions return the current state unchanged.
 */
class WalkStateMachine {

    fun transition(currentState: WalkState, event: WalkEvent): WalkState {
        return when (currentState) {
            is WalkState.Idle -> when (event) {
                is WalkEvent.StartWalk -> WalkState.Walking
                else -> currentState
            }

            is WalkState.Walking -> when (event) {
                is WalkEvent.SafetyTriggered -> WalkState.SafetyCheck
                is WalkEvent.EndWalk -> WalkState.Idle
                else -> currentState
            }

            is WalkState.SafetyCheck -> when (event) {
                is WalkEvent.SafetyConfirmed -> WalkState.Walking
                is WalkEvent.SafetyFailed -> WalkState.Emergency
                else -> currentState
            }

            is WalkState.Emergency -> when (event) {
                is WalkEvent.EmergencyResolved -> WalkState.Idle
                else -> currentState
            }
        }
    }
}

