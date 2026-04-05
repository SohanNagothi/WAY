package com.example.way.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.way.data.local.PrefsManager
import com.example.way.data.model.User
import com.example.way.data.repository.AuthRepository
import com.example.way.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel shared across auth screens (Login, Signup, Splash).
 * Handles Firebase authentication state and exposes results via LiveData.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val _authState = MutableLiveData<Result<User>?>(null)
    val authState: LiveData<Result<User>?> = _authState

    fun login(email: String, password: String) {
        _authState.value = Result.Loading
        viewModelScope.launch {
            _authState.value = authRepository.signInWithEmail(email, password)
        }
    }

    fun signup(name: String, email: String, password: String) {
        _authState.value = Result.Loading
        viewModelScope.launch {
            _authState.value = authRepository.signUpWithEmail(name, email, password)
        }
    }

    fun handleGoogleSignInResult(idToken: String) {
        _authState.value = Result.Loading
        viewModelScope.launch {
            _authState.value = authRepository.signInWithGoogle(idToken)
        }
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun isSetupComplete(): Boolean = prefsManager.isSetupComplete

    fun getCurrentUserName(): String {
        return authRepository.getCurrentUser()?.name
            ?: prefsManager.userName.ifEmpty { "User" }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = null
    }

    fun resetAuthState() {
        _authState.value = null
    }
}
