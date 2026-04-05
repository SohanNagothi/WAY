package com.example.way.data.repository

import com.example.way.data.model.WalkSession
import com.example.way.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for walk session history.
 * TODO Phase 2: Implement with Room (local) + Firestore (remote).
 */
interface WalkSessionRepository {
    fun getAllSessions(): Flow<List<WalkSession>>
    suspend fun getSession(sessionId: String): Result<WalkSession>
    suspend fun saveSession(session: WalkSession): Result<Unit>
    suspend fun getTotalWalkCount(): Int
    suspend fun getTotalAlertCount(): Int
    suspend fun getLastSession(): WalkSession?
    suspend fun removeDuplicateSessions(): Result<Int>
}
