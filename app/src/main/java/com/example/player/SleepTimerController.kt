package com.example.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SleepTimerMode {
    OFF,
    DURATION_MINUTES,
    EXACT_TIME,
    END_OF_TRACK,
    AFTER_TRACK_COUNT
}

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.OFF,
    val remainingMs: Long = 0L,
    val remainingTracks: Int = 0,
    val fadeOutEnabled: Boolean = true,
    val isFadingOut: Boolean = false,
    val warningShown: Boolean = false
)

class SleepTimerController(
    private val scope: CoroutineScope,
    private val onVolumeMultiplierChanged: (Float) -> Unit,
    private val onTimerExpired: () -> Unit
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var targetEndElapsedMs: Long = 0L

    fun startMinutes(minutes: Int, fadeOut: Boolean) {
        val durationMs = minutes.coerceAtLeast(1) * 60_000L
        startMillis(durationMs, SleepTimerMode.DURATION_MINUTES, fadeOut)
    }

    fun startUntilClockMillis(targetEpochMs: Long, fadeOut: Boolean) {
        val now = System.currentTimeMillis()
        val delta = (targetEpochMs - now).coerceAtLeast(15_000L)
        startMillis(delta, SleepTimerMode.EXACT_TIME, fadeOut)
    }

    fun startStopAfterCurrentTrack(fadeOut: Boolean) {
        cancelTimerJobOnly()
        onVolumeMultiplierChanged(1f)
        _state.value = SleepTimerState(
            mode = SleepTimerMode.END_OF_TRACK,
            remainingTracks = 1,
            fadeOutEnabled = fadeOut
        )
    }

    fun startAfterTrackCount(trackCount: Int, fadeOut: Boolean) {
        cancelTimerJobOnly()
        onVolumeMultiplierChanged(1f)
        _state.value = SleepTimerState(
            mode = SleepTimerMode.AFTER_TRACK_COUNT,
            remainingTracks = trackCount.coerceAtLeast(1),
            fadeOutEnabled = fadeOut
        )
    }

    fun onTrackTransitionCompleted(): Boolean {
        val cur = _state.value
        if (cur.mode == SleepTimerMode.END_OF_TRACK) {
            triggerStop()
            return true
        }
        if (cur.mode == SleepTimerMode.AFTER_TRACK_COUNT) {
            val left = cur.remainingTracks - 1
            if (left <= 0) {
                triggerStop()
                return true
            } else {
                _state.value = cur.copy(remainingTracks = left)
            }
        }
        return false
    }

    fun cancel() {
        cancelTimerJobOnly()
        onVolumeMultiplierChanged(1f)
        _state.value = SleepTimerState()
    }

    private fun startMillis(durationMs: Long, mode: SleepTimerMode, fadeOut: Boolean) {
        cancelTimerJobOnly()
        onVolumeMultiplierChanged(1f)
        targetEndElapsedMs = System.currentTimeMillis() + durationMs
        _state.value = SleepTimerState(
            mode = mode,
            remainingMs = durationMs,
            fadeOutEnabled = fadeOut
        )
        timerJob = scope.launch {
            while (true) {
                val left = targetEndElapsedMs - System.currentTimeMillis()
                if (left <= 0L) {
                    triggerStop()
                    break
                }
                val fadeWindowMs = 15_000L
                val fading = fadeOut && left <= fadeWindowMs
                val vol = if (fading) (left.toFloat() / fadeWindowMs).coerceIn(0.05f, 1f) else 1f
                onVolumeMultiplierChanged(vol)
                _state.value = _state.value.copy(
                    remainingMs = left,
                    isFadingOut = fading,
                    warningShown = left <= 30_000L
                )
                delay(500L)
            }
        }
    }

    private fun triggerStop() {
        cancelTimerJobOnly()
        onVolumeMultiplierChanged(1f)
        _state.value = SleepTimerState()
        onTimerExpired()
    }

    private fun cancelTimerJobOnly() {
        timerJob?.cancel()
        timerJob = null
    }
}
