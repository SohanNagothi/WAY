package com.example.way.data.repository

import com.example.way.data.local.PrefsManager
import com.example.way.data.model.WalkSession
import com.example.way.util.Constants
import com.example.way.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalkSessionRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefsManager: PrefsManager
) : WalkSessionRepository {

    private fun walksCollection() =
        firestore.collection(Constants.COLLECTION_USERS)
            .document(auth.currentUser?.uid ?: prefsManager.userUid)
            .collection(Constants.COLLECTION_WALKS)

    override fun getAllSessions(): Flow<List<WalkSession>> = callbackFlow {
        val listener = walksCollection()
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WalkSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getSession(sessionId: String): Result<WalkSession> {
        return try {
            val doc = walksCollection().document(sessionId).get().await()
            val session = doc.toObject(WalkSession::class.java)?.copy(id = doc.id)
                ?: return Result.Error("Session not found")
            Result.Success(session)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to get session", e)
        }
    }

    override suspend fun saveSession(session: WalkSession): Result<Unit> {
        return try {
            val docRef = if (session.id.isBlank()) {
                walksCollection().document()
            } else {
                walksCollection().document(session.id)
            }
            val sessionWithId = session.copy(id = docRef.id)
            val completed = withTimeoutOrNull(8000L) {
                docRef.set(sessionWithId).await()
                true
            } ?: false
            if (!completed) {
                Result.Error("Timed out while saving session")
            } else {
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to save session", e)
        }
    }

    override suspend fun getTotalWalkCount(): Int {
        return try {
            val snapshot = walksCollection().get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun getTotalAlertCount(): Int {
        return try {
            val snapshot = walksCollection()
                .whereEqualTo("alertTriggered", true)
                .get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun getLastSession(): WalkSession? {
        return try {
            val snapshot = walksCollection()
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
            snapshot.documents.firstOrNull()?.toObject(WalkSession::class.java)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun removeDuplicateSessions(): Result<Int> {
        return try {
            val snapshot = walksCollection().get().await()
            val docs = snapshot.documents
            if (docs.isEmpty()) return Result.Success(0)

            val groups = docs.groupBy { doc ->
                val uid = doc.getString("userId") ?: ""
                val start = doc.getLong("startTime") ?: 0L
                "$uid|$start"
            }

            var removed = 0
            for ((_, group) in groups) {
                if (group.size <= 1) continue

                val sorted = group.sortedByDescending { doc ->
                    doc.getLong("endTime") ?: 0L
                }
                val toDelete = sorted.drop(1)
                toDelete.forEach { dup ->
                    dup.reference.delete().await()
                    removed++
                }
            }
            Result.Success(removed)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to remove duplicates", e)
        }
    }
}
