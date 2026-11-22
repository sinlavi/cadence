package com.example.data.lyrics

import com.example.core.model.LrcLine
import com.example.core.model.LyricsCandidate
import com.example.core.model.LyricsStorageMode
import com.example.core.model.ParsedLrc
import com.example.data.db.TrackDao
import com.example.data.db.TrackEntity
import com.example.data.preferences.AppSettingsSnapshot
import com.example.data.tagger.TagEditorEngine
import com.example.player.PlaybackProgressState
import com.example.player.PlaybackUiState
import com.example.player.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class LyricsTimelineSnapshot(
    val activeLineIndex: Int = -1,
    val activeLine: LrcLine? = null,
    val nextLine: LrcLine? = null,
    val activeWordIndex: Int = -1,
    val lineProgress: Float = 0f
)

data class LyricsSyncState(
    val trackId: Long? = null,
    val trackTitle: String = "",
    val trackArtist: String = "",
    val rawLrc: String = "",
    val plainText: String = "",
    val parsed: ParsedLrc = ParsedLrc(),
    val lines: List<LrcLine> = emptyList(),
    val isSynced: Boolean = false,
    val activeLineIndex: Int = -1,
    val activeLine: LrcLine? = null,
    val nextLine: LrcLine? = null,
    val activeWordIndex: Int = -1,
    val lineProgress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlayingForTrack: Boolean = false,
    val manualScrollPaused: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private data class LoadedLyricsBuffer(
    val trackId: Long? = null,
    val trackTitle: String = "",
    val trackArtist: String = "",
    val rawLrc: String = "",
    val plainText: String = "",
    val parsed: ParsedLrc = ParsedLrc(),
    val lines: List<LrcLine> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsSyncEngine(
    private val playerConnection: PlayerConnection,
    private val lyricsRepository: LyricsRepository,
    private val trackDao: TrackDao,
    private val tagEditorEngine: TagEditorEngine,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val buffer = MutableStateFlow(LoadedLyricsBuffer())
    private val manualScrollPaused = MutableStateFlow(false)

    val state: StateFlow<LyricsSyncState> = combine(
        buffer,
        playerConnection.uiState,
        playerConnection.progressState,
        manualScrollPaused
    ) { buf, ui, prog, scrollPaused ->
        val playingSameTrack = ui.currentTrack?.id != null && ui.currentTrack.id == buf.trackId
        val effectivePos = if (playingSameTrack) prog.positionMs else 0L
        val effectiveDur = if (playingSameTrack) prog.durationMs else (ui.currentTrack?.durationMs ?: 0L)
        val isSynced = buf.lines.any { it.timeMs != null }

        val snap = if (playingSameTrack && isSynced) {
            computeActiveState(buf.lines, effectivePos, effectiveDur)
        } else {
            LyricsTimelineSnapshot()
        }

        LyricsSyncState(
            trackId = buf.trackId,
            trackTitle = buf.trackTitle,
            trackArtist = buf.trackArtist,
            rawLrc = buf.rawLrc,
            plainText = buf.plainText,
            parsed = buf.parsed,
            lines = buf.lines,
            isSynced = isSynced,
            activeLineIndex = snap.activeLineIndex,
            activeLine = snap.activeLine,
            nextLine = snap.nextLine,
            activeWordIndex = snap.activeWordIndex,
            lineProgress = snap.lineProgress,
            positionMs = effectivePos,
            durationMs = effectiveDur,
            isPlayingForTrack = playingSameTrack && ui.isPlaying,
            manualScrollPaused = scrollPaused,
            isLoading = buf.isLoading,
            errorMessage = buf.errorMessage
        )
    }.stateIn(externalScope, SharingStarted.Eagerly, LyricsSyncState())

    init {
        externalScope.launch {
            playerConnection.uiState
                .map { it.currentTrack }
                .distinctUntilChanged { a, b -> a?.id == b?.id }
                .collect { track ->
                    if (track != null && buffer.value.trackId != track.id) {
                        loadForTrack(track)
                    } else if (track == null && buffer.value.trackId == null) {
                        buffer.value = LoadedLyricsBuffer()
                    }
                }
        }
    }

    suspend fun loadForTrackId(trackId: Long): LoadedLyricsResult {
        val track = trackDao.getTrackById(trackId)
        if (track == null) {
            buffer.value = LoadedLyricsBuffer(trackId = trackId, errorMessage = "Track not found.")
            return LoadedLyricsResult(null, emptyList(), "")
        }
        return loadForTrack(track)
    }

    suspend fun loadForTrack(track: TrackEntity): LoadedLyricsResult {
        buffer.value = LoadedLyricsBuffer(
            trackId = track.id,
            trackTitle = track.title,
            trackArtist = track.artist,
            isLoading = true
        )
        manualScrollPaused.value = false
        return runCatching {
            val tagRead = tagEditorEngine.readTagsFromTrack(track)
            val entity = lyricsRepository.loadBestLocalLyrics(track, tagRead?.tags?.lyrics)
            if (entity != null) {
                val raw = entity.syncedLrc.ifBlank { entity.plainLyrics }
                val parsed = LrcParser.parse(raw)
                val plain = entity.plainLyrics.ifBlank { parsed.lines.joinToString("\n") { it.text } }
                buffer.value = LoadedLyricsBuffer(
                    trackId = track.id,
                    trackTitle = track.title,
                    trackArtist = track.artist,
                    rawLrc = raw,
                    plainText = plain,
                    parsed = parsed,
                    lines = parsed.lines,
                    isLoading = false
                )
                LoadedLyricsResult(track, parsed.lines, plain)
            } else {
                buffer.value = LoadedLyricsBuffer(
                    trackId = track.id,
                    trackTitle = track.title,
                    trackArtist = track.artist,
                    isLoading = false
                )
                LoadedLyricsResult(track, emptyList(), "")
            }
        }.getOrElse { err ->
            buffer.value = LoadedLyricsBuffer(
                trackId = track.id,
                trackTitle = track.title,
                trackArtist = track.artist,
                isLoading = false,
                errorMessage = err.localizedMessage ?: "Failed to read lyrics."
            )
            LoadedLyricsResult(track, emptyList(), "")
        }
    }

    fun loadRawLrc(rawLrc: String, track: TrackEntity? = null) {
        val parsed = LrcParser.parse(rawLrc)
        val plain = parsed.lines.joinToString("\n") { it.text }
        buffer.value = buffer.value.copy(
            trackId = track?.id ?: buffer.value.trackId,
            trackTitle = track?.title ?: buffer.value.trackTitle,
            trackArtist = track?.artist ?: buffer.value.trackArtist,
            rawLrc = rawLrc,
            plainText = plain,
            parsed = parsed,
            lines = parsed.lines,
            isLoading = false,
            errorMessage = null
        )
        manualScrollPaused.value = false
    }

    fun updateLines(lines: List<LrcLine>, plainText: String? = null) {
        val current = buffer.value
        val serialized = LrcParser.serialize(lines, current.trackTitle.ifBlank { null }, current.trackArtist.ifBlank { null })
        val parsed = ParsedLrc(
            title = current.parsed.title ?: current.trackTitle.ifBlank { null },
            artist = current.parsed.artist ?: current.trackArtist.ifBlank { null },
            album = current.parsed.album,
            offsetMs = current.parsed.offsetMs,
            lines = lines,
            isSynced = lines.any { it.timeMs != null }
        )
        buffer.value = current.copy(
            rawLrc = serialized,
            plainText = plainText ?: lines.joinToString("\n") { it.text },
            parsed = parsed,
            lines = lines,
            errorMessage = null
        )
    }

    fun stampLine(index: Int, positionMs: Long): Int {
        val lines = buffer.value.lines.toMutableList()
        if (index !in lines.indices) return index
        lines[index] = lines[index].copy(timeMs = positionMs.coerceAtLeast(0L))
        updateLines(lines)
        return (index + 1).coerceAtMost(lines.lastIndex)
    }

    fun shiftOffset(deltaMs: Long) {
        updateLines(LrcParser.shiftAllTimestamps(buffer.value.lines, deltaMs))
    }

    fun addEmptyLine(): Int {
        val next = buffer.value.lines + LrcLine(UUID.randomUUID().toString(), null, "")
        updateLines(next)
        return next.lastIndex
    }

    fun seekToLine(line: LrcLine, track: TrackEntity?) {
        val tMs = line.timeMs ?: return
        val currentId = playerConnection.uiState.value.currentTrack?.id
        if (currentId == (track?.id ?: buffer.value.trackId)) {
            playerConnection.seekTo(tMs)
        } else if (track != null) {
            playerConnection.playTracks(listOf(track), 0)
            playerConnection.seekTo(tMs)
        }
        manualScrollPaused.value = false
    }

    fun pauseAutoScroll() {
        manualScrollPaused.value = true
    }

    fun resumeAutoScroll() {
        manualScrollPaused.value = false
    }

    suspend fun saveForTrack(
        track: TrackEntity,
        linesToSave: List<LrcLine>,
        plainText: String,
        storageMode: LyricsStorageMode,
        userEdited: Boolean = true,
        source: String = "user_editor"
    ): Boolean {
        val serialized = LrcParser.serialize(linesToSave, track.title, track.artist, track.album)
        val parsed = LrcParser.parse(serialized)
        val synced = if (parsed.isSynced) serialized else ""
        val ok = lyricsRepository.saveLyrics(
            track = track,
            plainLyrics = plainText,
            syncedLrc = synced,
            source = source,
            storageMode = storageMode,
            userEdited = userEdited,
            writeEmbeddedCallback = { trk, content ->
                val existing = tagEditorEngine.readTagsFromTrack(trk)?.tags
                    ?: com.example.core.model.EditableTrackTags(title = trk.title, artist = trk.artist, album = trk.album)
                tagEditorEngine.writeSingleTrackTags(trk, existing.copy(lyrics = content))
                true
            }
        )
        if (ok && buffer.value.trackId == track.id) {
            updateLines(parsed.lines.ifEmpty { linesToSave }, plainText)
        }
        return ok
    }

    suspend fun applyCandidate(
        track: TrackEntity,
        candidate: LyricsCandidate,
        storageMode: LyricsStorageMode,
        settings: AppSettingsSnapshot
    ): ParsedLrc {
        val chosenRaw = candidate.syncedLyrics?.takeIf { it.isNotBlank() } ?: candidate.plainLyrics.orEmpty()
        val parsed = LrcParser.parse(chosenRaw)
        val plain = candidate.plainLyrics ?: parsed.lines.joinToString("\n") { it.text }
        updateLines(parsed.lines, plain)
        if (settings.lyricsAutoSave) {
            lyricsRepository.saveLyrics(
                track = track,
                plainLyrics = plain,
                syncedLrc = candidate.syncedLyrics.orEmpty(),
                source = candidate.provider.name,
                storageMode = storageMode,
                userEdited = false
            )
        }
        return parsed
    }

    data class LoadedLyricsResult(
        val track: TrackEntity?,
        val lines: List<LrcLine>,
        val plainText: String
    )

    companion object {
        fun computeActiveState(
            lines: List<LrcLine>,
            positionMs: Long,
            durationMs: Long
        ): LyricsTimelineSnapshot {
            if (lines.isEmpty()) return LyricsTimelineSnapshot()
            val idx = LrcParser.findActiveLineIndex(lines, positionMs)
            if (idx < 0) {
                return LyricsTimelineSnapshot(
                    activeLineIndex = -1,
                    activeLine = null,
                    nextLine = lines.firstOrNull { it.timeMs != null }
                )
            }
            val currentLine = lines[idx]
            val nextLine = lines.drop(idx + 1).firstOrNull { it.timeMs != null }
            val startMs = currentLine.timeMs ?: 0L
            val endMs = nextLine?.timeMs ?: durationMs.takeIf { it > startMs } ?: (startMs + 4000L)
            val span = (endMs - startMs).coerceAtLeast(1L)
            val progress = ((positionMs - startMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            val activeWordIdx = if (currentLine.words.isNotEmpty()) {
                currentLine.words.indexOfLast { it.timeMs <= positionMs }
            } else -1
            return LyricsTimelineSnapshot(
                activeLineIndex = idx,
                activeLine = currentLine,
                nextLine = nextLine,
                activeWordIndex = activeWordIdx,
                lineProgress = progress
            )
        }
    }
}
