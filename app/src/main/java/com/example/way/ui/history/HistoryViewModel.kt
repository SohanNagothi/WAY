package com.example.way.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.way.data.model.WalkSession
import com.example.way.data.repository.WalkSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val walkSessionRepository: WalkSessionRepository
) : ViewModel() {

    private val _sessions = MutableLiveData<List<WalkSession>>()
    val sessions: LiveData<List<WalkSession>> = _sessions

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            walkSessionRepository.getAllSessions().collectLatest { list ->
                _sessions.value = list
                _isLoading.value = false
            }
        }
    }
}
