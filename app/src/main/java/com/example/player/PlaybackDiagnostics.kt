package com.example.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackErrorRecord(
    val timestampMs: Long = System.currentTimeMillis(),
    val trackId: Long,
    val message: String
)

class PlaybackDiagnostics {
    private val _errors = MutableStateFlow<List<PlaybackErrorRecord>>(emptyList())
    val errors: StateFlow<List<PlaybackErrorRecord>> = _errors.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    fun recordError(trackId: Long, message: String) {
        val rec = PlaybackErrorRecord(trackId = trackId, message = message)
        _errors.value = (listOf(rec) + _errors.value).take(50)
        _lastErrorMessage.value = message
    }

    fun dismissBanner() {
        _lastErrorMessage.value = null
    }

    fun clearAll() {
        _errors.value = emptyList()
        _lastErrorMessage.value = null
    }
}
