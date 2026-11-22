package com.example.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.CadenceApplication
import com.example.MainActivity
import com.example.R
import com.example.core.model.RepeatModeOption
import com.example.data.preferences.AppSettingsSnapshot
import androidx.core.content.ContextCompat
import com.example.data.db.PlaybackHistoryEntity
import com.example.data.db.QueueHistoryEntity
import com.example.data.db.TrackEntity
import com.example.widget.CadenceWidgetUpdater
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

@OptIn(UnstableApi::class)
class CadenceMediaService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var secondaryCrossfadePlayer: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    private val app get() = application as CadenceApplication
    private val container get() = app.container

    private var latestSettings: AppSettingsSnapshot = AppSettingsSnapshot()
    private var currentTrackId: Long = -1L
    private var currentTrackStartedAtMs: Long = 0L
    private var lastTrackDurationMs: Long = 0L
    private var currentReplayGainFactor: Float = 1.0f
    private var currentPerTrackVolume: Float = 1.0f
    private var sleepTimerVolumeFactor: Float = 1.0f
    private var crossfadeFactor: Float = 1.0f
    private var crossfadeJob: Job? = null
    private var positionSaveJob: Job? = null
    private var currentTrackIsFavorite: Boolean = false
    private var currentTrackArtworkUri: String? = null
    private var currentTrackContentUri: String? = null
    private var noisyReceiverRegistered = false
    private var widgetSyncReceiverRegistered = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                serviceScope.launch {
                    val settings = container.settingsRepository.settingsFlow.first()
                    if (settings.pauseOnHeadsetDisconnect && ::player.isInitialized && player.isPlaying) {
                        player.pause()
                    }
                }
            }
        }
    }

    private val widgetSyncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != CadenceWidgetUpdater.ACTION_SERVICE_WIDGET_SYNC) return
            val cmd = intent.getStringExtra(CadenceWidgetUpdater.EXTRA_COMMAND) ?: return
            serviceScope.launch {
                val settings = container.settingsRepository.settingsFlow.first()
                when (cmd) {
                    CadenceWidgetUpdater.ACTION_TOGGLE_SHUFFLE -> {
                        if (::player.isInitialized) {
                            player.shuffleModeEnabled = settings.shuffleEnabled
                            pushWidgetSnapshot()
                        }
                    }
                    CadenceWidgetUpdater.ACTION_CYCLE_REPEAT -> {
                        if (::player.isInitialized) {
                            player.repeatMode = when (settings.repeatMode) {
                                RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
                                RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
                                RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
                            }
                            pushWidgetSnapshot()
                        }
                    }
                    CadenceWidgetUpdater.ACTION_TOGGLE_FAVORITE -> {
                        container.playerConnection.refreshCurrentTrackFromDb()
                        val tid = currentTrackId
                        if (tid > 0L) {
                            val track = withContext(Dispatchers.IO) {
                                container.database.trackDao().getTrackById(tid)
                            }
                            currentTrackIsFavorite = track?.isFavorite ?: currentTrackIsFavorite
                            applyNotificationCustomization()
                            pushWidgetSnapshot()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        container.sleepTimerController = SleepTimerController(
            scope = serviceScope,
            onVolumeMultiplierChanged = { factor ->
                sleepTimerVolumeFactor = factor
                applyEffectiveVolume()
            },
            onTimerExpired = {
                if (::player.isInitialized && player.isPlaying) {
                    player.pause()
                }
            }
        )

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachAudioEffects(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    attachAudioEffects(player.audioSessionId)
                }
                if (playbackState == Player.STATE_ENDED) {
                    recordTrackFinish(skipped = false)
                    container.sleepTimerController?.onTrackTransitionCompleted()
                }
                pushWidgetSnapshot()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startPositionPersistenceLoop()
                } else {
                    persistCurrentPosition()
                }
                pushWidgetSnapshot()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                pushWidgetSnapshot()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                pushWidgetSnapshot()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (currentTrackId > 0 && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    recordTrackFinish(skipped = false)
                    val stopped = container.sleepTimerController?.onTrackTransitionCompleted() == true
                    if (stopped) return
                } else if (currentTrackId > 0 && reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    val elapsed = (System.currentTimeMillis() - currentTrackStartedAtMs)
                    val wasShort = elapsed < (lastTrackDurationMs * 0.45f) && lastTrackDurationMs > 15_000L
                    recordTrackFinish(skipped = wasShort)
                }
                val newId = mediaItem?.mediaId?.toLongOrNull() ?: -1L
                onTrackActivated(newId)
                pushWidgetSnapshot()
            }

            override fun onPlayerError(error: PlaybackException) {
                container.playbackDiagnostics.recordError(
                    trackId = currentTrackId,
                    message = "Codec/playback error: ${error.errorCodeName} — ${error.localizedMessage ?: "Unsupported or missing file"}"
                )
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        })

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, CadenceLibraryCallback())
            .setSessionActivity(sessionActivity)
            .build()

        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        noisyReceiverRegistered = true
        ContextCompat.registerReceiver(
            this,
            widgetSyncReceiver,
            IntentFilter(CadenceWidgetUpdater.ACTION_SERVICE_WIDGET_SYNC),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        widgetSyncReceiverRegistered = true

        observeSettingsAndRestoreQueue()
    }

    private fun pushWidgetSnapshot() {
        if (!::player.isInitialized) return
        CadenceWidgetUpdater.updateFromPlayer(
            context = this,
            player = player,
            trackId = currentTrackId,
            artworkUri = currentTrackArtworkUri,
            contentUri = currentTrackContentUri,
            isFavorite = currentTrackIsFavorite
        )
    }

    private fun observeSettingsAndRestoreQueue() {
        serviceScope.launch {
            var firstEmission = true
            container.settingsRepository.settingsFlow.collect { settings ->
                latestSettings = settings
                if (firstEmission) {
                    firstEmission = false
                    player.repeatMode = when (settings.repeatMode) {
                        RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
                        RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
                        RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
                    }
                    player.shuffleModeEnabled = settings.shuffleEnabled
                    player.skipSilenceEnabled = settings.skipSilence
                    player.playbackParameters = PlaybackParameters(settings.playbackSpeed, settings.playbackPitch)

                    // Restore persisted queue on startup
                    val queueIds = container.database.queueDao().getQueueTrackIdsOnce()
                    if (queueIds.isNotEmpty()) {
                        val tracksById = container.database.trackDao().getTracksByIds(queueIds).associateBy { it.id }
                        val ordered = queueIds.mapNotNull { tracksById[it] }
                        if (ordered.isNotEmpty()) {
                            val items = ordered.map { it.toMediaItem() }
                            val startIdx = ordered.indexOfFirst { it.id == settings.lastPlayedTrackId }.coerceAtLeast(0)
                            val startPos = if (settings.resumeTrackPosition) settings.lastPlayedPositionMs.coerceAtLeast(0L) else 0L
                            player.setMediaItems(items, startIdx, startPos)
                            player.prepare()
                            ordered.getOrNull(startIdx)?.let { onTrackActivated(it.id, updateHistory = false) }
                        }
                    }
                }
                applyNotificationCustomization()
            }
        }
    }

    private fun buildNotificationPlayerCommands(settings: AppSettingsSnapshot): Player.Commands {
        val builder = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
        if (!settings.notificationShowPrevious) {
            builder.remove(Player.COMMAND_SEEK_TO_PREVIOUS)
            builder.remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        }
        if (!settings.notificationShowNext) {
            builder.remove(Player.COMMAND_SEEK_TO_NEXT)
            builder.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        }
        return builder.build()
    }

    private fun buildNotificationCustomButtons(settings: AppSettingsSnapshot): List<CommandButton> {
        val list = mutableListOf<CommandButton>()
        if (settings.notificationShowSeekBack) {
            list += CommandButton.Builder(CommandButton.ICON_SKIP_BACK)
                .setDisplayName("Rewind ${settings.seekIntervalSec}s")
                .setCustomIconResId(R.drawable.ic_notif_replay_10)
                .setSessionCommand(SessionCommand(CMD_NOTIF_SEEK_BACK, Bundle.EMPTY))
                .build()
        }
        if (settings.notificationShowFavorite) {
            val favIcon = if (currentTrackIsFavorite) {
                CommandButton.ICON_HEART_FILLED
            } else {
                CommandButton.ICON_HEART_UNFILLED
            }
            list += CommandButton.Builder(favIcon)
                .setDisplayName(if (currentTrackIsFavorite) "Unlike" else "Like")
                .setSessionCommand(SessionCommand(CMD_NOTIF_TOGGLE_FAVORITE, Bundle.EMPTY))
                .build()
        }
        if (settings.notificationShowSeekForward) {
            list += CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD)
                .setDisplayName("Forward ${settings.seekIntervalSec}s")
                .setCustomIconResId(R.drawable.ic_notif_forward_10)
                .setSessionCommand(SessionCommand(CMD_NOTIF_SEEK_FORWARD, Bundle.EMPTY))
                .build()
        }
        return list
    }

    private fun applyNotificationCustomization() {
        val session = mediaLibrarySession ?: return
        val settings = latestSettings
        val customButtons = buildNotificationCustomButtons(settings)
        val notifPlayerCommands = buildNotificationPlayerCommands(settings)
        val notifSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            .add(SessionCommand(CMD_REFRESH_SETTINGS, Bundle.EMPTY))
            .add(SessionCommand(CMD_NOTIF_TOGGLE_FAVORITE, Bundle.EMPTY))
            .add(SessionCommand(CMD_NOTIF_SEEK_BACK, Bundle.EMPTY))
            .add(SessionCommand(CMD_NOTIF_SEEK_FORWARD, Bundle.EMPTY))
            .add(SessionCommand(CMD_SYNC_CURRENT_METADATA, Bundle.EMPTY))
            .build()

        session.connectedControllers.forEach { ctrl ->
            if (session.isMediaNotificationController(ctrl)) {
                session.setAvailableCommands(ctrl, notifSessionCommands, notifPlayerCommands)
                session.setCustomLayout(ctrl, customButtons)
            }
        }
    }

    private fun attachAudioEffects(sessionId: Int) {
        if (sessionId <= 0) return
        serviceScope.launch {
            val s = container.settingsRepository.settingsFlow.first()
            container.audioEffectsController.attachToSession(
                audioSessionId = sessionId,
                enabled = s.equalizerEnabled,
                savedBandsCsv = s.equalizerBandsCsv,
                bassStrength = s.bassBoostStrength,
                virtStrength = s.virtualizerStrength,
                loudnessMb = s.loudnessGainMb,
                presetName = s.equalizerSelectedPreset
            )
        }
    }

    private fun onTrackActivated(trackId: Long, updateHistory: Boolean = true) {
        currentTrackId = trackId
        currentTrackStartedAtMs = System.currentTimeMillis()
        if (trackId <= 0L) return
        serviceScope.launch(Dispatchers.IO) {
            val track = container.database.trackDao().getTrackById(trackId) ?: return@launch
            lastTrackDurationMs = track.durationMs
            currentTrackIsFavorite = track.isFavorite
            currentTrackArtworkUri = track.artworkUri
            currentTrackContentUri = track.contentUri
            val settings = container.settingsRepository.settingsFlow.first()
            val rgDb = track.replayGainTrackDb ?: track.replayGainAlbumDb
            currentReplayGainFactor = if (settings.replayGainEnabled && rgDb != null) {
                (10.0.pow((rgDb / 20.0).toDouble())).toFloat().coerceIn(0.25f, 1.5f)
            } else {
                1.0f
            }
            currentPerTrackVolume = track.perTrackVolumeGain.coerceIn(0.2f, 1.5f)
            if (updateHistory) {
                container.database.queueDao().insertQueueHistory(QueueHistoryEntity(trackId = trackId))
            }
            serviceScope.launch(Dispatchers.Main) {
                val targetSpeed = track.perTrackSpeed ?: settings.playbackSpeed
                player.playbackParameters = PlaybackParameters(targetSpeed, settings.playbackPitch)
                applyEffectiveVolume()
                triggerFadeInIfEnabled(settings.fadeOnPauseResume)
                applyNotificationCustomization()
                pushWidgetSnapshot()
            }
        }
    }

    private fun triggerFadeInIfEnabled(enabled: Boolean) {
        if (!enabled) {
            crossfadeFactor = 1f
            applyEffectiveVolume()
            return
        }
        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            crossfadeFactor = 0.15f
            applyEffectiveVolume()
            val steps = 8
            for (i in 1..steps) {
                delay(45L)
                crossfadeFactor = 0.15f + (0.85f * (i.toFloat() / steps))
                applyEffectiveVolume()
            }
        }
    }

    private fun applyEffectiveVolume() {
        if (!::player.isInitialized) return
        val combined = (currentReplayGainFactor * currentPerTrackVolume * sleepTimerVolumeFactor * crossfadeFactor)
            .coerceIn(0f, 1f)
        player.volume = combined
    }

    private fun startPositionPersistenceLoop() {
        positionSaveJob?.cancel()
        positionSaveJob = serviceScope.launch {
            while (player.isPlaying) {
                persistCurrentPosition()
                delay(4000L)
            }
        }
    }

    private fun persistCurrentPosition() {
        val id = currentTrackId
        if (id <= 0L || !::player.isInitialized) return
        val pos = player.currentPosition.coerceAtLeast(0L)
        serviceScope.launch(Dispatchers.IO) {
            container.settingsRepository.savePlaybackPosition(id, pos)
            container.database.trackDao().updateResumePosition(id, pos)
        }
    }

    private fun recordTrackFinish(skipped: Boolean) {
        val trackId = currentTrackId
        if (trackId <= 0L || currentTrackStartedAtMs <= 0L) return
        val listenedMs = (System.currentTimeMillis() - currentTrackStartedAtMs).coerceAtLeast(0L)
        if (listenedMs < 2000L) return
        serviceScope.launch(Dispatchers.IO) {
            val settings = container.settingsRepository.settingsFlow.first()
            if (!settings.listeningStatsEnabled) return@launch
            val dayBucket = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            if (skipped) {
                container.database.trackDao().recordSkip(trackId, listenedMs)
            } else {
                container.database.trackDao().recordPlayCompletion(trackId, System.currentTimeMillis(), listenedMs)
            }
            container.database.historyAndStatsDao().insertHistory(
                PlaybackHistoryEntity(
                    trackId = trackId,
                    startedAt = currentTrackStartedAtMs,
                    listenedMs = listenedMs,
                    completed = !skipped,
                    skipped = skipped,
                    dayBucket = dayBucket
                )
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        persistCurrentPosition()
        if (!::player.isInitialized || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        persistCurrentPosition()
        positionSaveJob?.cancel()
        crossfadeJob?.cancel()
        container.sleepTimerController?.cancel()
        container.audioEffectsController.release()
        if (noisyReceiverRegistered) {
            runCatching { unregisterReceiver(becomingNoisyReceiver) }
        }
        if (widgetSyncReceiverRegistered) {
            runCatching { unregisterReceiver(widgetSyncReceiver) }
        }
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        secondaryCrossfadePlayer?.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class CadenceLibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_REFRESH_SETTINGS, Bundle.EMPTY))
                .add(SessionCommand(CMD_NOTIF_TOGGLE_FAVORITE, Bundle.EMPTY))
                .add(SessionCommand(CMD_NOTIF_SEEK_BACK, Bundle.EMPTY))
                .add(SessionCommand(CMD_NOTIF_SEEK_FORWARD, Bundle.EMPTY))
                .add(SessionCommand(CMD_SYNC_CURRENT_METADATA, Bundle.EMPTY))
                .build()
            val isNotif = session.isMediaNotificationController(controller)
            val playerCommands = if (isNotif) {
                buildNotificationPlayerCommands(latestSettings)
            } else {
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            }
            val builder = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
            if (isNotif) {
                builder.setCustomLayout(buildNotificationCustomButtons(latestSettings))
            }
            return builder.build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_REFRESH_SETTINGS -> {
                    serviceScope.launch {
                        val s = container.settingsRepository.settingsFlow.first()
                        latestSettings = s
                        player.skipSilenceEnabled = s.skipSilence
                        player.playbackParameters = PlaybackParameters(s.playbackSpeed, s.playbackPitch)
                        applyNotificationCustomization()
                    }
                }
                CMD_NOTIF_TOGGLE_FAVORITE -> {
                    val tid = currentTrackId
                    if (tid > 0L) {
                        serviceScope.launch {
                            val next = !currentTrackIsFavorite
                            withContext(Dispatchers.IO) {
                                container.database.trackDao().setFavorite(tid, next)
                            }
                            currentTrackIsFavorite = next
                            applyNotificationCustomization()
                            container.playerConnection.refreshCurrentTrackFromDb()
                            CadenceWidgetUpdater.updateFavoriteState(this@CadenceMediaService, tid, next)
                            pushWidgetSnapshot()
                        }
                    }
                }
                CMD_NOTIF_SEEK_BACK -> {
                    if (::player.isInitialized) {
                        val delta = latestSettings.seekIntervalSec.coerceAtLeast(1) * 1000L
                        player.seekTo((player.currentPosition - delta).coerceAtLeast(0L))
                    }
                }
                CMD_NOTIF_SEEK_FORWARD -> {
                    if (::player.isInitialized) {
                        val delta = latestSettings.seekIntervalSec.coerceAtLeast(1) * 1000L
                        val dur = player.duration.coerceAtLeast(0L)
                        val target = if (dur > 0L) (player.currentPosition + delta).coerceAtMost(dur) else player.currentPosition + delta
                        player.seekTo(target)
                    }
                }
                CMD_SYNC_CURRENT_METADATA -> {
                    val tid = currentTrackId
                    if (tid > 0L) {
                        serviceScope.launch {
                            val updated = withContext(Dispatchers.IO) {
                                container.database.trackDao().getTrackById(tid)
                            }
                            if (updated != null) {
                                currentTrackIsFavorite = updated.isFavorite
                                currentTrackArtworkUri = updated.artworkUri
                                currentTrackContentUri = updated.contentUri
                                if (::player.isInitialized) {
                                    val curIdx = player.currentMediaItemIndex
                                    if (curIdx in 0 until player.mediaItemCount &&
                                        player.getMediaItemAt(curIdx).mediaId == tid.toString()
                                    ) {
                                        player.replaceMediaItem(curIdx, updated.toMediaItem())
                                    }
                                }
                                applyNotificationCustomization()
                                pushWidgetSnapshot()
                            }
                        }
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        // Android Auto / System Media Browser Root
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Cadence Library")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                ).build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch(Dispatchers.IO) {
                val items: List<MediaItem> = when (parentId) {
                    ROOT_ID -> listOf(
                        browsableFolder(NODE_SONGS, "Songs"),
                        browsableFolder(NODE_RECENT, "Recently Played"),
                        browsableFolder(NODE_FAVORITES, "Favorites"),
                        browsableFolder(NODE_PLAYLISTS, "Playlists")
                    )
                    NODE_SONGS -> container.database.trackDao().getAllAvailableTracks().take(200).map { it.toMediaItem() }
                    NODE_RECENT -> container.database.trackDao().getAllAvailableTracks()
                        .filter { it.lastPlayedAt > 0 }
                        .sortedByDescending { it.lastPlayedAt }
                        .take(50)
                        .map { it.toMediaItem() }
                    NODE_FAVORITES -> container.database.trackDao().getAllAvailableTracks()
                        .filter { it.isFavorite }
                        .map { it.toMediaItem() }
                    NODE_PLAYLISTS -> container.database.playlistDao().getAllPlaylistsOnce().map { pl ->
                        browsableFolder("playlist:${pl.id}", pl.name)
                    }
                    else -> if (parentId.startsWith("playlist:")) {
                        val id = parentId.removePrefix("playlist:").toLongOrNull() ?: 0L
                        container.database.playlistDao().getPlaylistTracksOnce(id).map { it.toMediaItem() }
                    } else emptyList()
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) item
                else {
                    item.buildUpon().setUri(item.requestMetadata.mediaUri).build()
                }
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }
    }

    private fun browsableFolder(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            ).build()
    }

    companion object {
        const val ROOT_ID = "cadence_root"
        const val NODE_SONGS = "cadence_songs"
        const val NODE_RECENT = "cadence_recent"
        const val NODE_FAVORITES = "cadence_favorites"
        const val NODE_PLAYLISTS = "cadence_playlists"
        const val CMD_REFRESH_SETTINGS = "com.example.CMD_REFRESH_SETTINGS"
        const val CMD_NOTIF_TOGGLE_FAVORITE = "com.example.CMD_NOTIF_TOGGLE_FAVORITE"
        const val CMD_NOTIF_SEEK_BACK = "com.example.CMD_NOTIF_SEEK_BACK"
        const val CMD_NOTIF_SEEK_FORWARD = "com.example.CMD_NOTIF_SEEK_FORWARD"
        const val CMD_SYNC_CURRENT_METADATA = "com.example.CMD_SYNC_CURRENT_METADATA"
    }
}

fun TrackEntity.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong("trackId", id)
        putString("codec", codec)
        putInt("bitrateKbps", bitrateKbps)
    }
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(contentUri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setAlbumArtist(albumArtist.ifBlank { artist })
                .setGenre(genre)
                .setTrackNumber(trackNumber.takeIf { it > 0 })
                .setDiscNumber(discNumber.takeIf { it > 0 })
                .setArtworkUri(artworkUri?.let { Uri.parse(it) })
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setExtras(extras)
                .build()
        )
        .build()
}
