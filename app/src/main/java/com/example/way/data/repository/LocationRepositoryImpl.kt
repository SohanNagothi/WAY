package com.example.way.data.repository

import android.util.Log
import com.example.way.data.local.PrefsManager
import com.example.way.data.model.FrequentLocation
import com.example.way.util.Constants
import com.example.way.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefsManager: PrefsManager
) : LocationRepository {

    companion object {
        private const val TAG = "LocationRepository"
    }

    private fun currentUid(): String {
        val uid = auth.currentUser?.uid ?: prefsManager.userUid
        require(uid.isNotBlank()) { "User not authenticated. Please sign in again." }
        return uid
    }

    private fun locationsCollection() =
        firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUid())
            .collection(Constants.COLLECTION_LOCATIONS)

    override fun getLocations(): Flow<List<FrequentLocation>> = callbackFlow {
        val uid = try {
            currentUid()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot observe locations", e)
            trySend(emptyList())
            close(e)
            return@callbackFlow
        }

        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .collection(Constants.COLLECTION_LOCATIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore location listener failed", error)
                    return@addSnapshotListener
                }
                val locations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FrequentLocation::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(locations)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addLocation(location: FrequentLocation): Result<Unit> {
        return try {
            val docRef = locationsCollection().document()
            val loc = location.copy(id = docRef.id)
            val completed = withTimeoutOrNull(8000L) {
                docRef.set(loc).await()
                true
            } ?: false
            if (!completed) {
                Result.Error("Timed out while saving location. Check internet and try again.")
            } else {
                Log.d(TAG, "Location added: ${loc.name} at ${loc.latitude},${loc.longitude}")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "addLocation failed", e)
            val message = e.localizedMessage ?: "Failed to add location"
            if (message.contains("PERMISSION_DENIED", ignoreCase = true)) {
                Result.Error("Firestore permission denied for locations. Update Firebase rules for users/{uid}/locations.", e)
            } else {
                Result.Error(message, e)
            }
        }
    }

    override suspend fun updateLocation(location: FrequentLocation): Result<Unit> {
        return try {
            if (location.id.isBlank()) {
                return Result.Error("Location ID cannot be empty")
            }
            val completed = withTimeoutOrNull(8000L) {
                locationsCollection().document(location.id).set(location).await()
                true
            } ?: false
            if (!completed) {
                Result.Error("Timed out while updating location. Check internet and try again.")
            } else {
                Log.d(TAG, "Location updated: ${location.name} at ${location.latitude},${location.longitude}")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateLocation failed", e)
            Result.Error(e.localizedMessage ?: "Failed to update location", e)
        }
    }

    override suspend fun deleteLocation(locationId: String): Result<Unit> {
        return try {
            val completed = withTimeoutOrNull(8000L) {
                locationsCollection().document(locationId).delete().await()
                true
            } ?: false
            if (!completed) {
                Result.Error("Timed out while deleting location. Check internet and try again.")
            } else {
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteLocation failed", e)
            val message = e.localizedMessage ?: "Failed to delete location"
            if (message.contains("PERMISSION_DENIED", ignoreCase = true)) {
                Result.Error("Firestore permission denied for locations. Update Firebase rules for users/{uid}/locations.", e)
            } else {
                Result.Error(message, e)
            }
        }
    }
}
