package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.core.model.RepeatModeOption
import com.example.data.db.QueueDao
import com.example.data.db.TrackDao
import com.example.data.db.TrackEntity
import com.example.data.preferences.SettingsRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val currentMediaIndex: Int = 0,
    val queueSize: Int = 0,
    val abLoopStartMs: Long? = null,
    val abLoopEndMs: Long? = null
)

data class PlaybackProgressState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class PlayerConnection(
    private val context: Context,
    private val trackDao: TrackDao,
    private val queueDao: QueueDao,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _progressState = MutableStateFlow(PlaybackProgressState())
    val progressState: StateFlow<PlaybackProgressState> = _progressState.asStateFlow()

    fun connect() {
        if (controller != null || controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, CadenceMediaService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val mc = future.get()
                controller = mc
                attachListener(mc)
                syncStateFromController(mc)
                startProgressLoop()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener(mc: MediaController) {
        mc.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncStateFromController(mc)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _uiState.value = _uiState.value.copy(abLoopStartMs = null, abLoopEndMs = null)
                syncStateFromController(mc)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncStateFromController(mc)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                syncStateFromController(mc)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                syncStateFromController(mc)
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                syncStateFromController(mc)
            }
        })
    }

    private fun syncStateFromController(mc: MediaController) {
        val currentId = mc.currentMediaItem?.mediaId?.toLongOrNull()
        val repeat = when (mc.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
            Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
            else -> RepeatModeOption.OFF
        }
        _uiState.value = _uiState.value.copy(
            isPlaying = mc.isPlaying,
            shuffleEnabled = mc.shuffleModeEnabled,
            repeatMode = repeat,
            playbackSpeed = mc.playbackParameters.speed,
            playbackPitch = mc.playbackParameters.pitch,
            currentMediaIndex = mc.currentMediaItemIndex,
            queueSize = mc.mediaItemCount
        )
        _progressState.value = PlaybackProgressState(
            positionMs = mc.currentPosition.coerceAtLeast(0L),
            durationMs = mc.duration.coerceAtLeast(0L)
        )
        if (currentId != null && currentId != _uiState.value.currentTrack?.id) {
            scope.launch(Dispatchers.IO) {
                val track = trackDao.getTrackById(currentId)
                _uiState.value = _uiState.value.copy(currentTrack = track)
            }
        } else if (currentId == null) {
            _uiState.value = _uiState.value.copy(currentTrack = null)
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val mc = controller
                if (mc != null && mc.isPlaying) {
                    val pos = mc.currentPosition.coerceAtLeast(0L)
                    val dur = mc.duration.coerceAtLeast(_uiState.value.currentTrack?.durationMs ?: 0L)
                    val cur = _progressState.value
                    if (cur.positionMs != pos || cur.durationMs != dur) {
                        _progressState.value = PlaybackProgressState(pos, dur)
                    }
                    // Handle A-B loop repeat
                    val a = _uiState.value.abLoopStartMs
                    val b = _uiState.value.abLoopEndMs
                    if (a != null && b != null && b > a && pos >= b) {
                        mc.seekTo(a)
                    }
                    delay(150L)
                } else {
                    delay(500L)
                }
            }
        }
    }

    fun playTracks(tracks: List<TrackEntity>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val mc = controller ?: return
        val idx = startIndex.coerceIn(0, tracks.lastIndex)
        mc.shuffleModeEnabled = shuffle
        mc.setMediaItems(tracks.map { it.toMediaItem() }, idx, 0L)
        mc.prepare()
        mc.play()
        scope.launch(Dispatchers.IO) {
            queueDao.replaceQueue(tracks.map { it.id })
            settingsRepository.updateSettings { it.copy(shuffleEnabled = shuffle) }
        }
        syncStateFromController(mc)
    }

    fun playExternalUri(uri: Uri, fallbackTitle: String = "External Audio") {
        val mc = controller ?: return
        scope.launch(Dispatchers.IO) {
            val matched = trackDao.getTrackByUri(uri.toString())
            if (matched != null) {
                scope.launch(Dispatchers.Main.immediate) {
                    playTracks(listOf(matched), 0)
                }
            } else {
                val item = MediaItem.Builder()
                    .setMediaId("-1")
                    .setUri(uri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(uri.lastPathSegment?.substringAfterLast('/') ?: fallbackTitle)
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
                scope.launch(Dispatchers.Main.immediate) {
                    mc.setMediaItem(item)
                    mc.prepare()
                    mc.play()
                    syncStateFromController(mc)
                }
            }
        }
    }

    fun playTrackFromQueueIndex(index: Int) {
        val mc = controller ?: return
        if (index in 0 until mc.mediaItemCount) {
            mc.seekTo(index, 0L)
            mc.prepare()
            mc.play()
        }
    }

    fun togglePlayPause() {
        val mc = controller ?: return
        if (mc.isPlaying) mc.pause() else {
            if (mc.playbackState == Player.STATE_IDLE) mc.prepare()
            mc.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val mc = controller ?: return
        mc.seekTo(positionMs.coerceAtLeast(0L))
        _progressState.value = _progressState.value.copy(positionMs = positionMs.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val mc = controller ?: return
        val target = (mc.currentPosition + deltaMs).coerceIn(0L, mc.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun next() {
        val mc = controller ?: return
        if (mc.hasNextMediaItem()) {
            mc.seekToNextMediaItem()
            mc.prepare()
            mc.play()
        }
    }

    fun previous() {
        val mc = controller ?: return
        if (mc.currentPosition > 3500L) {
            mc.seekTo(0L)
        } else if (mc.hasPreviousMediaItem()) {
            mc.seekToPreviousMediaItem()
            mc.prepare()
            mc.play()
        } else {
            mc.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val mc = controller ?: return
        val next = !mc.shuffleModeEnabled
        mc.shuffleModeEnabled = next
        scope.launch { settingsRepository.updateSettings { it.copy(shuffleEnabled = next) } }
    }

    fun cycleRepeatMode() {
        val mc = controller ?: return
        val nextOption = when (_uiState.value.repeatMode) {
            RepeatModeOption.OFF -> RepeatModeOption.ALL
            RepeatModeOption.ALL -> RepeatModeOption.ONE
            RepeatModeOption.ONE -> RepeatModeOption.OFF
        }
        mc.repeatMode = when (nextOption) {
            RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
        }
        scope.launch { settingsRepository.updateSettings { it.copy(repeatMode = nextOption) } }
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        val mc = controller ?: return
        val s = speed.coerceIn(0.5f, 2.5f)
        val p = pitch.coerceIn(0.5f, 2.0f)
        mc.playbackParameters = PlaybackParameters(s, p)
        scope.launch { settingsRepository.updateSettings { it.copy(playbackSpeed = s, playbackPitch = p) } }
    }

    fun setAbLoopPoint(isStart: Boolean) {
        val pos = _progressState.value.positionMs
        val cur = _uiState.value
        _uiState.value = if (isStart) {
            cur.copy(abLoopStartMs = pos, abLoopEndMs = cur.abLoopEndMs?.takeIf { it > pos })
        } else {
            val start = cur.abLoopStartMs ?: 0L
            if (pos > start) cur.copy(abLoopStartMs = start, abLoopEndMs = pos) else cur
        }
    }

    fun clearAbLoop() {
        _uiState.value = _uiState.value.copy(abLoopStartMs = null, abLoopEndMs = null)
    }

    fun addPlayNext(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val mc = controller ?: return
        scope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val existingIds = queueDao.getQueueTrackIdsOnce().toMutableList()
            val filtered = if (settings.preventQueueDuplicates) {
                tracks.filter { it.id !in existingIds }
            } else tracks
            if (filtered.isEmpty()) return@launch

            if (mc.mediaItemCount == 0) {
                playTracks(filtered, 0)
                return@launch
            }
            val insertAt = (mc.currentMediaItemIndex + 1).coerceAtMost(mc.mediaItemCount)
            mc.addMediaItems(insertAt, filtered.map { it.toMediaItem() })
            existingIds.addAll(insertAt.coerceAtMost(existingIds.size), filtered.map { it.id })
            queueDao.replaceQueue(existingIds)
            syncStateFromController(mc)
        }
    }

    fun addToEnd(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val mc = controller ?: return
        scope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val existingIds = queueDao.getQueueTrackIdsOnce().toMutableList()
            val filtered = if (settings.preventQueueDuplicates) {
                tracks.filter { it.id !in existingIds }
            } else tracks
            if (filtered.isEmpty()) return@launch

            if (mc.mediaItemCount == 0) {
                playTracks(filtered, 0)
                return@launch
            }
            mc.addMediaItems(filtered.map { it.toMediaItem() })
            existingIds.addAll(filtered.map { it.id })
            queueDao.replaceQueue(existingIds)
            syncStateFromController(mc)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val mc = controller ?: return
        if (fromIndex == toIndex || fromIndex !in 0 until mc.mediaItemCount || toIndex !in 0 until mc.mediaItemCount) return
        mc.moveMediaItem(fromIndex, toIndex)
        scope.launch(Dispatchers.IO) {
            val ids = queueDao.getQueueTrackIdsOnce().toMutableList()
            if (fromIndex in ids.indices && toIndex in ids.indices) {
                val moved = ids.removeAt(fromIndex)
                ids.add(toIndex, moved)
                queueDao.replaceQueue(ids)
            }
        }
        syncStateFromController(mc)
    }

    fun removeQueueItem(index: Int) {
        val mc = controller ?: return
        if (index !in 0 until mc.mediaItemCount) return
        mc.removeMediaItem(index)
        scope.launch(Dispatchers.IO) {
            val ids = queueDao.getQueueTrackIdsOnce().toMutableList()
            if (index in ids.indices) {
                ids.removeAt(index)
                queueDao.replaceQueue(ids)
            }
        }
        syncStateFromController(mc)
    }

    fun clearQueue() {
        val mc = controller ?: return
        mc.stop()
        mc.clearMediaItems()
        scope.launch(Dispatchers.IO) {
            queueDao.clearQueue()
        }
        syncStateFromController(mc)
    }

    fun notifyServiceSettingsChanged() {
        controller?.sendCustomCommand(SessionCommand(CadenceMediaService.CMD_REFRESH_SETTINGS, Bundle.EMPTY), Bundle.EMPTY)
    }

    fun refreshCurrentTrackFromDb(syncMediaMetadata: Boolean = false) {
        val id = _uiState.value.currentTrack?.id ?: return
        scope.launch(Dispatchers.IO) {
            val refreshed = trackDao.getTrackById(id)
            _uiState.value = _uiState.value.copy(currentTrack = refreshed)
            if (syncMediaMetadata) {
                scope.launch(Dispatchers.Main.immediate) {
                    controller?.sendCustomCommand(
                        SessionCommand(CadenceMediaService.CMD_SYNC_CURRENT_METADATA, Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                }
            }
        }
    }
}
