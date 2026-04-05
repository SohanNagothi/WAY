package com.example.way.service

import android.location.Location
import com.example.way.data.model.WalkEvent
import com.example.way.data.model.WalkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central state holder for an active walk session.
 * Shared between WalkForegroundService and UI (WalkViewModel).
 *
 * This is a singleton — Hilt creates one instance for the whole app.
 * Both the foreground service and fragments observe the same state.
 */
@Singleton
class WalkSessionManager @Inject constructor() {

    private val stateMachine = WalkStateMachine()

    private val _walkState = MutableStateFlow<WalkState>(WalkState.Idle)
    val walkState: StateFlow<WalkState> = _walkState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _distanceToDestination = MutableStateFlow(0f)
    val distanceToDestination: StateFlow<Float> = _distanceToDestination.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    private val _totalDistanceMeters = MutableStateFlow(0f)
    val totalDistanceMeters: StateFlow<Float> = _totalDistanceMeters.asStateFlow()

    var destinationName: String = ""
        private set
    var destinationLat: Double = 0.0
        private set
    var destinationLng: Double = 0.0
        private set
    var startTime: Long = 0L
        private set
    var alertTriggered: Boolean = false
        private set
    var triggerType: String? = null
        private set

    /**
     * Process a walk event through the state machine.
     * Returns the new state after the transition.
     */
    fun processEvent(event: WalkEvent): WalkState {
        val newState = stateMachine.transition(_walkState.value, event)
        _walkState.value = newState
        if (event is WalkEvent.SafetyTriggered) {
            alertTriggered = true
            triggerType = event.trigger::class.simpleName
        }
        return newState
    }

    fun setDestination(name: String, lat: Double = 0.0, lng: Double = 0.0) {
        destinationName = name
        destinationLat = lat
        destinationLng = lng
    }

    fun updateLocation(location: Location) {
        val prevLocation = _currentLocation.value
        _currentLocation.value = location
        if (prevLocation != null) {
            _totalDistanceMeters.value += prevLocation.distanceTo(location)
        }
    }

    fun updateDistance(distanceMeters: Float) {
        _distanceToDestination.value = distanceMeters
    }

    fun updateElapsedTime(seconds: Long) {
        _elapsedTimeSeconds.value = seconds
    }

    fun markStartTime() {
        startTime = System.currentTimeMillis()
    }

    /**
     * Reset everything when a walk ends or emergency is resolved.
     */
    fun reset() {
        _walkState.value = WalkState.Idle
        _currentLocation.value = null
        _distanceToDestination.value = 0f
        _elapsedTimeSeconds.value = 0L
        _totalDistanceMeters.value = 0f
        destinationName = ""
        destinationLat = 0.0
        destinationLng = 0.0
        startTime = 0L
        alertTriggered = false
        triggerType = null
    }
}
