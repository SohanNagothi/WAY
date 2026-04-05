package com.example.way.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.way.data.local.PrefsManager
import com.example.way.data.model.WalkSession
import com.example.way.data.repository.WalkSessionRepository
import com.example.way.util.Result
import com.google.gson.Gson
import com.google.gson.Strictness
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.StringReader
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val walkSessionRepository: WalkSessionRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val _totalWalks = MutableLiveData(0)
    val totalWalks: LiveData<Int> = _totalWalks

    private val _totalAlerts = MutableLiveData(0)
    val totalAlerts: LiveData<Int> = _totalAlerts

    private val _lastSession = MutableLiveData<WalkSession?>()
    val lastSession: LiveData<WalkSession?> = _lastSession

    private val gson = Gson()

    init {
        retryCachedSession()
        cleanupDuplicatesThenLoad()
    }

    private fun cleanupDuplicatesThenLoad() {
        viewModelScope.launch {
            walkSessionRepository.removeDuplicateSessions()
            loadStats()
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            _totalWalks.value = walkSessionRepository.getTotalWalkCount()
            _totalAlerts.value = walkSessionRepository.getTotalAlertCount()
            _lastSession.value = walkSessionRepository.getLastSession()
        }
    }

    /**
     * If the previous walk failed to upload (no internet), retry now.
     */
    private fun retryCachedSession() {
        val json = prefsManager.cachedWalkSessionJson
        if (json.isBlank()) return

        viewModelScope.launch {
            try {
                val reader = JsonReader(StringReader(json)).apply {
                    // Cached payload can be partially malformed; prefer tolerant recovery.
                    setStrictness(Strictness.LENIENT)
                }
                val type = TypeToken.get(WalkSession::class.java)
                val cached = gson.fromJson<WalkSession>(reader, type) ?: return@launch

                val stableId = if (cached.id.isBlank() && cached.userId.isNotBlank() && cached.startTime > 0L) {
                    "${cached.userId}_${cached.startTime}"
                } else {
                    cached.id
                }
                val session = if (stableId.isNotBlank()) cached.copy(id = stableId) else cached

                val result = walkSessionRepository.saveSession(session)
                if (result is Result.Success) {
                    prefsManager.clearCachedWalkSession()
                    Log.d("DashboardVM", "Cached walk session uploaded successfully")
                    loadStats() // refresh
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Failed to retry cached session", e)
            }
        }
    }
}
