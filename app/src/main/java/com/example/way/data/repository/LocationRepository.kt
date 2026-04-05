package com.example.way.data.repository

import com.example.way.data.model.FrequentLocation
import com.example.way.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for frequent/saved locations.
 * TODO Phase 2: Implement with Room (local) + Firestore (remote).
 */
interface LocationRepository {
    fun getLocations(): Flow<List<FrequentLocation>>
    suspend fun addLocation(location: FrequentLocation): Result<Unit>
    suspend fun updateLocation(location: FrequentLocation): Result<Unit>
    suspend fun deleteLocation(locationId: String): Result<Unit>
}

