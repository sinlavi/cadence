package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.db.BookmarkEntity
import com.example.data.db.CadenceDatabase
import com.example.data.db.CustomThemeEntity
import com.example.data.db.EqualizerPresetEntity
import com.example.data.db.LyricsEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.SmartPlaylistEntity
import com.example.data.db.TrackEntity
import com.example.data.preferences.AppSettingsSnapshot
import com.example.data.preferences.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupPlaylistEntry(
    val name: String,
    val description: String,
    val trackUris: List<String>,
    val trackTitlesAndArtists: List<String>
)

@Serializable
data class BackupSmartPlaylistEntry(
    val name: String,
    val description: String,
    val definitionJson: String,
    val isBuiltIn: Boolean
)

@Serializable
data class BackupTrackUserMeta(
    val contentUri: String,
    val title: String,
    val artist: String,
    val isFavorite: Boolean,
    val rating: Int,
    val playCount: Int,
    val skipCount: Int,
    val totalListenMs: Long,
    val lastPlayedAt: Long
)

@Serializable
data class BackupLyricsRecord(
    val fallbackKey: String,
    val plainLyrics: String,
    val syncedLrc: String,
    val isSynced: Boolean,
    val source: String,
    val userEdited: Boolean
)

@Serializable
data class BackupCustomThemeRecord(
    val name: String,
    val configJson: String
)

@Serializable
data class BackupEqPresetRecord(
    val name: String,
    val bandLevelsCsv: String,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val loudnessGainMb: Int
)

@Serializable
data class CadenceBackupManifest(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: AppSettingsSnapshot,
    val playlists: List<BackupPlaylistEntry>,
    val smartPlaylists: List<BackupSmartPlaylistEntry>,
    val trackUserMeta: List<BackupTrackUserMeta>,
    val lyrics: List<BackupLyricsRecord>,
    val customThemes: List<BackupCustomThemeRecord>,
    val customEqPresets: List<BackupEqPresetRecord>
)

class BackupManager(
    private val context: Context,
    private val db: CadenceDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportZipBackup(targetUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsRepository.settingsFlow.first()
            // Never export user-entered private tokens in plain backups
            val sanitizedSettings = settings.copy(
                appleMusicTokenOverride = "",
                discogsTokenOverride = "",
                acoustIdKeyOverride = "",
                musixmatchKeyOverride = ""
            )
            val playlists = db.playlistDao().getAllPlaylistsOnce().map { pl ->
                val tracks = db.playlistDao().getPlaylistTracksOnce(pl.id)
                BackupPlaylistEntry(
                    name = pl.name,
                    description = pl.description,
                    trackUris = tracks.map { it.contentUri },
                    trackTitlesAndArtists = tracks.map { "${it.artist} - ${it.title}" }
                )
            }
            val smartPlaylists = db.playlistDao().getAllSmartPlaylistsOnce().map { sp ->
                BackupSmartPlaylistEntry(sp.name, sp.description, sp.definitionJson, sp.isBuiltIn)
            }
            val tracks = db.trackDao().getAllAvailableTracks()
            val trackMeta = tracks.filter {
                it.isFavorite || it.rating > 0 || it.playCount > 0 || it.skipCount > 0
            }.map { t ->
                BackupTrackUserMeta(
                    contentUri = t.contentUri,
                    title = t.title,
                    artist = t.artist,
                    isFavorite = t.isFavorite,
                    rating = t.rating,
                    playCount = t.playCount,
                    skipCount = t.skipCount,
                    totalListenMs = t.totalListenMs,
                    lastPlayedAt = t.lastPlayedAt
                )
            }
            val lyrics = db.lyricsDao().getAllLyricsOnce().map { l ->
                BackupLyricsRecord(l.fallbackKey, l.plainLyrics, l.syncedLrc, l.isSynced, l.source, l.userEdited)
            }
            val themes = db.customizationAndCacheDao().getAllCustomThemesOnce().map {
                BackupCustomThemeRecord(it.name, it.configJson)
            }
            val eqs = db.customizationAndCacheDao().getAllEqPresetsOnce().filter { !it.isBuiltIn }.map {
                BackupEqPresetRecord(it.name, it.bandLevelsCsv, it.bassBoostStrength, it.virtualizerStrength, it.loudnessGainMb)
            }

            val manifest = CadenceBackupManifest(
                settings = sanitizedSettings,
                playlists = playlists,
                smartPlaylists = smartPlaylists,
                trackUserMeta = trackMeta,
                lyrics = lyrics,
                customThemes = themes,
                customEqPresets = eqs
            )
            val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                ZipOutputStream(BufferedOutputStream(out)).use { zos ->
                    zos.putNextEntry(ZipEntry("cadence_backup_manifest.json"))
                    zos.write(manifestBytes)
                    zos.closeEntry()
                }
            } ?: error("Could not open backup destination.")

            "Exported ${playlists.size} playlists, ${smartPlaylists.size} smart playlists, and ${lyrics.size} lyrics."
        }
    }

    suspend fun importZipBackup(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var manifestJson: String? = null
            context.contentResolver.openInputStream(sourceUri)?.use { inp ->
                ZipInputStream(BufferedInputStream(inp)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "cadence_backup_manifest.json") {
                            manifestJson = zis.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            } ?: error("Unable to open backup file.")

            val raw = manifestJson ?: error("Invalid backup archive: cadence_backup_manifest.json missing.")
            val manifest = json.decodeFromString<CadenceBackupManifest>(raw)
            if (manifest.schemaVersion > 1) {
                error("Backup was created by a newer Cadence version (schema v${manifest.schemaVersion}).")
            }

            settingsRepository.updateSettings { cur ->
                manifest.settings.copy(
                    appleMusicTokenOverride = cur.appleMusicTokenOverride,
                    discogsTokenOverride = cur.discogsTokenOverride,
                    acoustIdKeyOverride = cur.acoustIdKeyOverride,
                    musixmatchKeyOverride = cur.musixmatchKeyOverride
                )
            }

            val allTracks = db.trackDao().getAllAvailableTracks()
            val byUri = allTracks.associateBy { it.contentUri }
            val byTitleArtist = allTracks.associateBy { "${it.artist.lowercase()} - ${it.title.lowercase()}" }

            var restoredPlaylists = 0
            for (pl in manifest.playlists) {
                val id = db.playlistDao().insertPlaylist(PlaylistEntity(name = pl.name, description = pl.description))
                val resolvedIds = pl.trackUris.mapIndexedNotNull { idx, uri ->
                    byUri[uri]?.id ?: pl.trackTitlesAndArtists.getOrNull(idx)?.lowercase()?.let { byTitleArtist[it]?.id }
                }
                db.playlistDao().replacePlaylistTracks(id, resolvedIds)
                restoredPlaylists++
            }

            for (sp in manifest.smartPlaylists) {
                if (!sp.isBuiltIn) {
                    db.playlistDao().upsertSmartPlaylist(
                        SmartPlaylistEntity(name = sp.name, description = sp.description, definitionJson = sp.definitionJson)
                    )
                }
            }

            for (meta in manifest.trackUserMeta) {
                val target = byUri[meta.contentUri] ?: byTitleArtist["${meta.artist.lowercase()} - ${meta.title.lowercase()}"]
                if (target != null) {
                    db.trackDao().updateTrack(
                        target.copy(
                            isFavorite = meta.isFavorite,
                            rating = meta.rating,
                            playCount = maxOf(target.playCount, meta.playCount),
                            skipCount = maxOf(target.skipCount, meta.skipCount),
                            totalListenMs = maxOf(target.totalListenMs, meta.totalListenMs),
                            lastPlayedAt = maxOf(target.lastPlayedAt, meta.lastPlayedAt)
                        )
                    )
                }
            }

            for (l in manifest.lyrics) {
                val matchingTrack = allTracks.firstOrNull {
                    "${it.artist.lowercase()}::${it.title.lowercase()}" == l.fallbackKey
                }
                if (matchingTrack != null) {
                    db.lyricsDao().upsertLyrics(
                        LyricsEntity(
                            trackId = matchingTrack.id,
                            fallbackKey = l.fallbackKey,
                            plainLyrics = l.plainLyrics,
                            syncedLrc = l.syncedLrc,
                            isSynced = l.isSynced,
                            source = l.source,
                            userEdited = l.userEdited
                        )
                    )
                    db.trackDao().setHasLyrics(matchingTrack.id, true)
                }
            }

            for (theme in manifest.customThemes) {
                db.customizationAndCacheDao().upsertCustomTheme(CustomThemeEntity(name = theme.name, configJson = theme.configJson))
            }
            for (eq in manifest.customEqPresets) {
                db.customizationAndCacheDao().upsertEqPreset(
                    EqualizerPresetEntity(
                        name = eq.name,
                        bandLevelsCsv = eq.bandLevelsCsv,
                        bassBoostStrength = eq.bassBoostStrength,
                        virtualizerStrength = eq.virtualizerStrength,
                        loudnessGainMb = eq.loudnessGainMb,
                        isBuiltIn = false
                    )
                )
            }

            "Restored $restoredPlaylists playlists, ${manifest.smartPlaylists.size} smart rules, and user ratings/settings."
        }
    }

    suspend fun exportM3u8Playlist(playlistName: String, tracks: List<TrackEntity>, targetUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sb = StringBuilder("#EXTM3U\n#PLAYLIST:$playlistName\n")
                for (t in tracks) {
                    val sec = (t.durationMs / 1000L).coerceAtLeast(0L)
                    sb.appendLine("#EXTINF:$sec,${t.artist} - ${t.title}")
                    sb.appendLine(t.filePath.ifBlank { t.contentUri })
                }
                context.contentResolver.openOutputStream(targetUri, "wt")?.bufferedWriter()?.use {
                    it.write(sb.toString())
                } ?: error("Unable to open export URI.")
                Unit
            }
        }

    suspend fun importM3uPlaylist(playlistName: String, sourceUri: Uri): Result<Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read M3U file.")
                val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
                val allTracks = db.trackDao().getAllAvailableTracks()
                val byPath = allTracks.associateBy { it.filePath }
                val byUri = allTracks.associateBy { it.contentUri }
                val byFileName = allTracks.associateBy { it.fileName.lowercase() }

                val matchedIds = mutableListOf<Long>()
                var missingCount = 0
                for (ref in lines) {
                    val fileName = ref.substringAfterLast('/').substringAfterLast('\\').lowercase()
                    val found = byUri[ref] ?: byPath[ref] ?: byFileName[fileName]
                    if (found != null) {
                        matchedIds.add(found.id)
                    } else {
                        missingCount++
                    }
                }
                val id = db.playlistDao().insertPlaylist(PlaylistEntity(name = playlistName))
                db.playlistDao().replacePlaylistTracks(id, matchedIds)
                matchedIds.size to missingCount
            }
        }

    suspend fun exportLibraryCsv(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val tracks = db.trackDao().getAllAvailableTracks()
            val sb = StringBuilder("Title,Artist,Album,AlbumArtist,Genre,Year,Track,DurationMs,Format,BitrateKbps,PlayCount,Rating,Path\n")
            fun esc(s: String): String = "\"${s.replace("\"", "\"\"")}\""
            for (t in tracks) {
                sb.appendLine(
                    listOf(
                        esc(t.title), esc(t.artist), esc(t.album), esc(t.albumArtist),
                        esc(t.genre), t.year.toString(), t.trackNumber.toString(),
                        t.durationMs.toString(), esc(t.format), t.bitrateKbps.toString(),
                        t.playCount.toString(), t.rating.toString(), esc(t.filePath.ifBlank { t.contentUri })
                    ).joinToString(",")
                )
            }
            context.contentResolver.openOutputStream(targetUri, "wt")?.bufferedWriter()?.use {
                it.write(sb.toString())
            } ?: error("Unable to write CSV export.")
            tracks.size
        }
    }
}
