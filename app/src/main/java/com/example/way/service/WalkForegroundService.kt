package com.example.way.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.way.R
import com.example.way.data.model.SafetyTrigger
import com.example.way.data.model.WalkEvent
import com.example.way.data.model.WalkState
import com.example.way.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class WalkForegroundService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "WalkService"
        const val ACTION_START = "com.example.way.START_WALK"
        const val ACTION_STOP = "com.example.way.STOP_WALK"
    }

    @Inject lateinit var walkSessionManager: WalkSessionManager
    @Inject lateinit var emergencyHandler: EmergencyHandler
    @Inject lateinit var prefsManager: com.example.way.data.local.PrefsManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // Sensors
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // Safety tracking
    private var lastMovementTime = System.currentTimeMillis()
    private var movementEvidenceReadings = 0
    private var distanceIncreasingCount = 0
    private var highSpeedReadings = 0
    private var lastDistanceToDestination = Float.MAX_VALUE
    private var lastSafetyTriggerTime = 0L

    // Fall detection
    private var freeFallDetected = false
    private var freeFallTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startWalkMonitoring()
            ACTION_STOP -> stopWalkMonitoring()
        }
        return START_STICKY
    }

    private fun startWalkMonitoring() {
        createNotificationChannel()
        // Reset per-session trigger trackers.
        lastMovementTime = System.currentTimeMillis()
        movementEvidenceReadings = 0
        distanceIncreasingCount = 0
        highSpeedReadings = 0
        lastDistanceToDestination = Float.MAX_VALUE
        lastSafetyTriggerTime = 0L

        val notification = NotificationCompat.Builder(this, Constants.WALK_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle("WAY — Walk Active")
            .setContentText("Monitoring your walk to ${walkSessionManager.destinationName}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(Constants.WALK_SERVICE_NOTIFICATION_ID, notification)

        walkSessionManager.markStartTime()
        walkSessionManager.processEvent(WalkEvent.StartWalk)

        startLocationUpdates()
        startAccelerometer()
        startTimer()
        observeState()

        Log.d(TAG, "Walk monitoring started")
    }

    private fun stopWalkMonitoring() {
        timerJob?.cancel()
        stopLocationUpdates()
        stopAccelerometer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Walk monitoring stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.WALK_SERVICE_CHANNEL_ID,
                "Walk Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active walk session monitoring"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    // ── Location Tracking ──

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.WALK_LOCATION_UPDATE_INTERVAL_MS
        ).setMinUpdateIntervalMillis(Constants.WALK_FASTEST_LOCATION_INTERVAL_MS).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    processLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request, locationCallback!!, Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun processLocation(location: Location) {
        if (walkSessionManager.walkState.value !is WalkState.Walking) return

        // Ignore noisy GPS points that commonly break speed/distance checks.
        if (location.hasAccuracy() && location.accuracy > Constants.MIN_VALID_GPS_ACCURACY_METERS) {
            return
        }

        val previous = walkSessionManager.currentLocation.value
        walkSessionManager.updateLocation(location)

        val movedMeters = if (previous != null) previous.distanceTo(location) else 0f
        val currentSpeed = if (location.hasSpeed()) location.speed else 0f
        val distanceIndicatesMovement = movedMeters >= Constants.MIN_MOVEMENT_METERS
        val speedIndicatesMovement = location.hasSpeed() &&
            currentSpeed >= Constants.MIN_MOVEMENT_SPEED_MPS &&
            movedMeters >= (Constants.MIN_MOVEMENT_METERS * 0.4f)

        if (distanceIndicatesMovement || speedIndicatesMovement) {
            movementEvidenceReadings++
        } else {
            movementEvidenceReadings = 0
        }

        val now = System.currentTimeMillis()
        if (movementEvidenceReadings >= Constants.MOVEMENT_CONFIRMATION_READINGS) {
            lastMovementTime = now
            movementEvidenceReadings = 0
        }

        val inactiveDuration = (now - lastMovementTime) / 1000
        if (inactiveDuration >= Constants.INACTIVITY_THRESHOLD_SECONDS) {
            triggerSafetyIfAllowed(SafetyTrigger.Inactivity(inactiveDuration))
            lastMovementTime = now
            movementEvidenceReadings = 0
            return
        }

        if (currentSpeed > Constants.SPEED_THRESHOLD_MPS) {
            highSpeedReadings++
            if (highSpeedReadings >= Constants.SPEED_SUSTAINED_READINGS) {
                triggerSafetyIfAllowed(SafetyTrigger.SuddenSpeed(currentSpeed))
                highSpeedReadings = 0
                return
            }
        } else {
            highSpeedReadings = 0
        }

        if (walkSessionManager.destinationLat != 0.0 && walkSessionManager.destinationLng != 0.0) {
            val destLocation = Location("dest").apply {
                latitude = walkSessionManager.destinationLat
                longitude = walkSessionManager.destinationLng
            }
            val currentDist = location.distanceTo(destLocation)
            walkSessionManager.updateDistance(currentDist)

            if (lastDistanceToDestination != Float.MAX_VALUE) {
                val increasedEnough = currentDist > (lastDistanceToDestination + Constants.DISTANCE_INCREASE_TOLERANCE_METERS)
                if (increasedEnough) {
                    distanceIncreasingCount++
                    if (distanceIncreasingCount >= Constants.DISTANCE_INCREASING_COUNT) {
                        triggerSafetyIfAllowed(SafetyTrigger.DistanceIncreasing(currentDist))
                        distanceIncreasingCount = 0
                        return
                    }
                } else {
                    distanceIncreasingCount = 0
                }
            }
            lastDistanceToDestination = currentDist
        }
    }

    private fun triggerSafetyIfAllowed(trigger: SafetyTrigger) {
        val now = System.currentTimeMillis()
        if (now - lastSafetyTriggerTime < Constants.SAFETY_TRIGGER_COOLDOWN_MS) return
        lastSafetyTriggerTime = now
        walkSessionManager.processEvent(WalkEvent.SafetyTriggered(trigger))
    }

    // ── Accelerometer (Fall Detection) ──

    private fun startAccelerometer() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopAccelerometer() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        if (walkSessionManager.walkState.value !is WalkState.Walking) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        val now = System.currentTimeMillis()

        // Detect free-fall (very low acceleration)
        if (magnitude < Constants.FALL_MAGNITUDE_LOW) {
            freeFallDetected = true
            freeFallTime = now
        }

        // Detect impact after free-fall
        if (freeFallDetected && magnitude > Constants.FALL_MAGNITUDE_HIGH) {
            if (now - freeFallTime < Constants.FALL_DETECTION_WINDOW_MS) {
                walkSessionManager.processEvent(
                    WalkEvent.SafetyTriggered(SafetyTrigger.PhoneFall)
                )
                freeFallDetected = false
            }
        }

        // Reset if too much time passed
        if (freeFallDetected && now - freeFallTime > Constants.FALL_DETECTION_WINDOW_MS) {
            freeFallDetected = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── Timer ──

    private fun startTimer() {
        timerJob = serviceScope.launch {
            var seconds = 0L
            while (true) {
                delay(1000)
                seconds++
                walkSessionManager.updateElapsedTime(seconds)
            }
        }
    }

    // ── State Observer ──

    private fun observeState() {
        serviceScope.launch {
            walkSessionManager.walkState.collect { state ->
                when (state) {
                    is WalkState.Emergency -> {
                        handleEmergency()
                    }
                    is WalkState.Idle -> {
                        // Walk ended or emergency resolved
                    }
                    else -> { /* Walking or SafetyCheck — handled by UI */ }
                }
            }
        }
    }

    private fun handleEmergency() {
        serviceScope.launch {
            val location = walkSessionManager.currentLocation.value
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0

            emergencyHandler.showEmergencyNotification()
            emergencyHandler.sendEmergencySms(lat, lng)
            emergencyHandler.dialPriorityContact()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Fail-safe: cache the current walk session locally
        try {
            val uid = prefsManager.userUid
            val stableSessionId = "${uid}_${walkSessionManager.startTime}"
            val session = com.example.way.data.model.WalkSession(
                id = stableSessionId,
                userId = uid,
                destinationName = walkSessionManager.destinationName,
                destinationLat = walkSessionManager.destinationLat,
                destinationLng = walkSessionManager.destinationLng,
                startTime = walkSessionManager.startTime,
                endTime = System.currentTimeMillis(),
                durationSeconds = walkSessionManager.elapsedTimeSeconds.value,
                distanceMeters = walkSessionManager.totalDistanceMeters.value,
                alertTriggered = walkSessionManager.alertTriggered,
                triggerType = walkSessionManager.triggerType,
                date = walkSessionManager.startTime
            )
            prefsManager.cachedWalkSessionJson = com.google.gson.Gson().toJson(session)
            Log.d(TAG, "Walk session cached locally (app killed)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache walk on task removed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        stopLocationUpdates()
        stopAccelerometer()
        serviceScope.cancel()
    }
}
