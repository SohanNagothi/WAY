package com.example.way.data.repository

import com.example.way.data.local.PrefsManager
import com.example.way.data.model.User
import com.example.way.util.Constants
import com.example.way.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AuthRepository] using Firebase Auth + Firestore.
 *
 * On successful auth, caches user info in [PrefsManager] and writes a
 * user document to Firestore (creates only if it doesn't exist).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefsManager: PrefsManager
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.Error("Sign in failed: no user returned")

            // Fetch profile and repair setup flag if legacy users already have onboarding data.
            val user = buildResolvedUser(
                uid = firebaseUser.uid,
                fallbackName = firebaseUser.displayName ?: "",
                fallbackEmail = firebaseUser.email ?: email
            )
            cacheUser(user)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Sign in failed", e)
        }
    }

    override suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Result.Error("Sign up failed: no user returned")

            // Set display name on Firebase profile
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdate).await()

            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                setupComplete = false,
                createdAt = System.currentTimeMillis()
            )

            // Write user document to Firestore
            saveUserToFirestore(user)
            cacheUser(user)

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Sign up failed", e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
                ?: return Result.Error("Google sign-in failed: no user returned")

            val isNewUser = result.additionalUserInfo?.isNewUser == true

            val user = if (isNewUser) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    setupComplete = false,
                    createdAt = System.currentTimeMillis()
                )
                saveUserToFirestore(newUser)
                newUser
            } else {
                buildResolvedUser(
                    uid = firebaseUser.uid,
                    fallbackName = firebaseUser.displayName ?: "",
                    fallbackEmail = firebaseUser.email ?: ""
                )
            }

            cacheUser(user)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Google sign-in failed", e)
        }
    }

    override fun signOut() {
        auth.signOut()
        prefsManager.clear()
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName ?: prefsManager.userName,
            email = firebaseUser.email ?: ""
        )
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    // ── Helpers ──

    private suspend fun fetchUserFromFirestore(uid: String): User? {
        return try {
            withTimeoutOrNull(5000L) {
                val doc = firestore.collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .await()
                doc.toObject(User::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            withTimeoutOrNull(5000L) {
                firestore.collection(Constants.COLLECTION_USERS)
                    .document(user.uid)
                    .set(user)
                    .await()
            }
        } catch (_: Exception) {
            // Non-fatal: user can still use the app, data syncs later
        }
    }

    private fun cacheUser(user: User) {
        prefsManager.userUid = user.uid
        prefsManager.userName = user.name
        prefsManager.isSetupComplete = user.setupComplete
    }

    private suspend fun buildResolvedUser(
        uid: String,
        fallbackName: String,
        fallbackEmail: String
    ): User {
        val fromDb = fetchUserFromFirestore(uid)
        val base = fromDb ?: User(
            uid = uid,
            name = fallbackName,
            email = fallbackEmail,
            setupComplete = false,
            createdAt = System.currentTimeMillis()
        )

        val inferredSetupComplete = base.setupComplete || hasOnboardingData(uid)
        val resolved = base.copy(setupComplete = inferredSetupComplete)

        if (fromDb == null || fromDb.setupComplete != resolved.setupComplete) {
            saveUserToFirestore(resolved)
        }

        return resolved
    }

    private suspend fun hasOnboardingData(uid: String): Boolean {
        return try {
            val contactsSnap = withTimeoutOrNull(4000L) {
                firestore.collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .collection(Constants.COLLECTION_CONTACTS)
                    .limit(1)
                    .get()
                    .await()
            }
            contactsSnap?.isEmpty == false
        } catch (_: Exception) {
            false
        }
    }
}
