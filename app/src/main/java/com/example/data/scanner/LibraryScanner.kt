package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.core.util.AudioFormatSupport
import com.example.core.util.TextNormalizer
import com.example.data.db.CadenceDatabase
import com.example.data.db.ScanFolderEntity
import com.example.data.db.TrackEntity
import com.example.data.preferences.AppSettingsSnapshot
import com.example.data.preferences.SettingsRepository
import com.example.data.tagger.TagEditorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

data class ScanProgressState(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val updatedCount: Int = 0,
    val statusText: String = ""
)

class LibraryScanner(
    private val context: Context,
    private val db: CadenceDatabase,
    private val settingsRepository: SettingsRepository,
    private val tagEditorEngine: TagEditorEngine
) {
    private val _scanState = MutableStateFlow(ScanProgressState())
    val scanState: StateFlow<ScanProgressState> = _scanState.asStateFlow()

    suspend fun scanAll(forceMetadataRefresh: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        if (_scanState.value.isScanning) return@withContext Result.success(0)
        _scanState.value = ScanProgressState(isScanning = true, statusText = "Preparing audio scan...")
        runCatching {
            val settings = settingsRepository.settingsFlow.first()
            val scanFolders = db.customizationAndCacheDao().getScanFoldersOnce()
            val excludedPaths = scanFolders.filter { it.isExcluded }.map { it.uriOrPath.lowercase() }
            val existingByUri = db.trackDao().getAllAvailableTracks().associateBy { it.contentUri }

            val activeUris = mutableListOf<String>()
            val batchToUpsert = mutableListOf<TrackEntity>()
            var scanned = 0
            var updated = 0

            // 1. Scan MediaStore audio
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DISPLAY_NAME
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf((settings.minTrackDurationSec * 1000L).toString())

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val msId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, msId)
                    val uriStr = contentUri.toString()
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val fileName = (if (nameCol >= 0) cursor.getString(nameCol) else null)
                        ?: filePath.substringAfterLast('/').ifBlank { "audio_$msId" }
                    val folderPath = filePath.substringBeforeLast('/', "")
                    val folderName = folderPath.substringAfterLast('/', "Music")
                    val ext = fileName.substringAfterLast('.', "").uppercase()
                    val format = AudioFormatSupport.extensionToFormat(ext)

                    if (shouldSkipPath(folderPath, fileName, format, excludedPaths, settings)) continue

                    val durationMs = cursor.getLong(durCol).coerceAtLeast(0L)
                    if (durationMs < settings.minTrackDurationSec * 1000L) continue

                    activeUris.add(uriStr)
                    scanned++
                    val fileSize = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(addedCol)
                    val dateMod = cursor.getLong(modCol)
                    val existing = existingByUri[uriStr]

                    val unchanged = !forceMetadataRefresh &&
                        existing != null &&
                        existing.dateModifiedSec == dateMod &&
                        existing.fileSizeBytes == fileSize

                    if (unchanged) {
                        if (scanned % 40 == 0) {
                            _scanState.value = ScanProgressState(true, scanned, updated, "Checking $fileName")
                        }
                        continue
                    }

                    val albumId = cursor.getLong(albumIdCol)
                    val artUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else null

                    val fallbackTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: fileName.substringBeforeLast('.')
                    val fallbackArtist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Artist"
                    val fallbackAlbum = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Album"
                    val fallbackYear = if (yearCol >= 0) cursor.getInt(yearCol) else 0
                    val rawTrack = if (trackCol >= 0) cursor.getInt(trackCol) else 0

                    val entity = enrichWithTagLib(
                        base = TrackEntity(
                            id = existing?.id ?: 0L,
                            mediaStoreId = msId,
                            contentUri = uriStr,
                            filePath = filePath,
                            fileName = fileName,
                            folderPath = folderPath,
                            folderName = folderName,
                            title = fallbackTitle,
                            artist = fallbackArtist,
                            album = fallbackAlbum,
                            year = fallbackYear,
                            trackNumber = rawTrack % 1000,
                            discNumber = rawTrack / 1000,
                            durationMs = durationMs,
                            fileSizeBytes = fileSize,
                            dateAddedSec = dateAdded,
                            dateModifiedSec = dateMod,
                            format = format,
                            codec = format,
                            albumId = albumId,
                            artworkUri = artUri,
                            isFavorite = existing?.isFavorite ?: false,
                            rating = existing?.rating ?: 0,
                            playCount = existing?.playCount ?: 0,
                            skipCount = existing?.skipCount ?: 0,
                            totalListenMs = existing?.totalListenMs ?: 0L,
                            lastPlayedAt = existing?.lastPlayedAt ?: 0L,
                            lastResumePosMs = existing?.lastResumePosMs ?: 0L,
                            perTrackSpeed = existing?.perTrackSpeed,
                            perTrackVolumeGain = existing?.perTrackVolumeGain ?: 1.0f,
                            isAvailable = true,
                            lastScannedAt = System.currentTimeMillis()
                        ),
                        uri = contentUri
                    )
                    batchToUpsert.add(entity)
                    updated++

                    if (batchToUpsert.size >= 40) {
                        db.trackDao().upsertTracks(batchToUpsert.toList())
                        batchToUpsert.clear()
                        _scanState.value = ScanProgressState(true, scanned, updated, "Indexed $scanned tracks...")
                    }
                }
            }

            // 2. Scan user-added SAF tree folders
            for (folder in scanFolders.filter { it.isTreeUri && !it.isExcluded }) {
                val treeUri = Uri.parse(folder.uriOrPath)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: continue
                scanDocumentTree(
                    doc = rootDoc,
                    rootFolderName = folder.displayName,
                    settings = settings,
                    existingByUri = existingByUri,
                    activeUris = activeUris,
                    batch = batchToUpsert,
                    onProgress = { s, u, name ->
                        scanned += s
                        updated += u
                        _scanState.value = ScanProgressState(true, scanned, updated, "SAF: $name")
                    }
                )
            }

            if (batchToUpsert.isNotEmpty()) {
                db.trackDao().upsertTracks(batchToUpsert.toList())
            }
            if (activeUris.isNotEmpty()) {
                db.trackDao().markMissingExcept(activeUris)
            }
            _scanState.value = ScanProgressState(false, scanned, updated, "Completed: $scanned tracks indexed")
            scanned
        }.also {
            if (it.isFailure) {
                _scanState.value = ScanProgressState(false, 0, 0, "Scan failed: ${it.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    private suspend fun scanDocumentTree(
        doc: DocumentFile,
        rootFolderName: String,
        settings: AppSettingsSnapshot,
        existingByUri: Map<String, TrackEntity>,
        activeUris: MutableList<String>,
        batch: MutableList<TrackEntity>,
        onProgress: suspend (Int, Int, String) -> Unit
    ) {
        val children = doc.listFiles()
        if (settings.respectNomedia && children.any { it.name == ".nomedia" }) return

        for (child in children) {
            val name = child.name.orEmpty()
            if (!settings.includeHiddenFolders && name.startsWith(".")) continue
            if (child.isDirectory) {
                scanDocumentTree(child, name, settings, existingByUri, activeUris, batch, onProgress)
            } else if (child.isFile) {
                val ext = name.substringAfterLast('.', "").uppercase()
                val format = AudioFormatSupport.extensionToFormat(ext)
                if (format !in settings.enabledFormats && ext !in settings.enabledFormats) continue
                val uriStr = child.uri.toString()
                activeUris.add(uriStr)
                val modSec = child.lastModified() / 1000L
                val existing = existingByUri[uriStr]
                if (existing != null && existing.dateModifiedSec == modSec) {
                    onProgress(1, 0, name)
                    continue
                }
                val base = TrackEntity(
                    id = existing?.id ?: 0L,
                    contentUri = uriStr,
                    filePath = "",
                    fileName = name,
                    folderPath = doc.uri.toString(),
                    folderName = rootFolderName,
                    title = name.substringBeforeLast('.'),
                    format = format,
                    codec = format,
                    fileSizeBytes = child.length(),
                    dateAddedSec = modSec,
                    dateModifiedSec = modSec,
                    isAvailable = true,
                    lastScannedAt = System.currentTimeMillis()
                )
                val enriched = enrichWithTagLib(base, child.uri)
                if (enriched.durationMs >= settings.minTrackDurationSec * 1000L) {
                    batch.add(enriched)
                    onProgress(1, 1, name)
                    if (batch.size >= 30) {
                        db.trackDao().upsertTracks(batch.toList())
                        batch.clear()
                    }
                }
            }
        }
    }

    private suspend fun enrichWithTagLib(base: TrackEntity, uri: Uri): TrackEntity {
        val read = tagEditorEngine.readTagsFromUri(uri) ?: return base.copy(
            normalizedSearch = TextNormalizer.buildTrackSearchIndex(
                base.title, base.artist, base.album, base.albumArtist, base.genre, base.composer, base.folderName
            )
        )
        val t = read.tags
        val title = t.title.ifBlank { base.title }
        val artist = t.artist.ifBlank { base.artist }
        val album = t.album.ifBlank { base.album }
        val year = t.year.toIntOrNull() ?: t.releaseDate.take(4).toIntOrNull() ?: base.year
        val durationMs = if (base.durationMs > 0) base.durationMs else read.lengthSec * 1000L
        return base.copy(
            title = title,
            artist = artist,
            album = album,
            albumArtist = t.albumArtist,
            genre = t.genre,
            composer = t.composer,
            conductor = t.conductor,
            comment = t.comment,
            copyright = t.copyright,
            publisher = t.publisher,
            recordLabel = t.recordLabel,
            encoder = t.encoder,
            grouping = t.grouping,
            subtitle = t.subtitle,
            language = t.language,
            isrc = t.isrc,
            musicalKey = t.musicalKey,
            releaseDate = t.releaseDate,
            originalReleaseDate = t.originalReleaseDate,
            year = year,
            trackNumber = t.trackNumber.toIntOrNull() ?: base.trackNumber,
            totalTracks = t.totalTracks.toIntOrNull() ?: base.totalTracks,
            discNumber = t.discNumber.toIntOrNull() ?: base.discNumber,
            totalDiscs = t.totalDiscs.toIntOrNull() ?: base.totalDiscs,
            bpm = t.bpm.toIntOrNull() ?: base.bpm,
            isCompilation = t.isCompilation,
            durationMs = durationMs,
            bitrateKbps = read.bitrateKbps,
            sampleRateHz = read.sampleRateHz,
            channels = read.channels,
            replayGainTrackDb = read.replayGainTrackDb,
            replayGainAlbumDb = read.replayGainAlbumDb,
            hasEmbeddedArtwork = read.pictures.isNotEmpty(),
            hasLyrics = t.lyrics.isNotBlank() || base.hasLyrics,
            rating = if (base.rating > 0) base.rating else t.rating,
            normalizedSearch = TextNormalizer.buildTrackSearchIndex(
                title, artist, album, t.albumArtist, t.genre, t.composer, base.folderName
            )
        )
    }

    private fun shouldSkipPath(
        folderPath: String,
        fileName: String,
        format: String,
        excludedPaths: List<String>,
        settings: AppSettingsSnapshot
    ): Boolean {
        if (format !in settings.enabledFormats && fileName.substringAfterLast('.', "").uppercase() !in settings.enabledFormats) {
            return true
        }
        val lowerFolder = folderPath.lowercase()
        if (excludedPaths.any { lowerFolder.startsWith(it) }) return true
        if (!settings.includeHiddenFolders) {
            if (fileName.startsWith(".") || folderPath.split('/').any { it.startsWith(".") }) return true
        }
        if (settings.respectNomedia && folderPath.isNotBlank()) {
            if (File(folderPath, ".nomedia").exists()) return true
        }
        return false
    }

    suspend fun deleteTrackFile(track: TrackEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(track.contentUri)
            val deleted = if (track.filePath.isNotBlank() && File(track.filePath).exists()) {
                File(track.filePath).delete()
            } else {
                context.contentResolver.delete(uri, null, null) > 0
            }
            if (deleted) {
                db.trackDao().deleteTrackById(track.id)
                if (track.filePath.isNotBlank()) {
                    MediaScannerConnection.scanFile(context, arrayOf(track.filePath), null, null)
                }
            } else {
                error("File could not be deleted (storage provider rejected deletion).")
            }
        }
    }

    suspend fun addSafFolder(treeUri: Uri, isExcluded: Boolean) = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromTreeUri(context, treeUri)
        val name = doc?.name ?: treeUri.lastPathSegment ?: "Folder"
        db.customizationAndCacheDao().upsertScanFolder(
            ScanFolderEntity(
                uriOrPath = treeUri.toString(),
                displayName = name,
                isTreeUri = true,
                isExcluded = isExcluded
            )
        )
    }

    suspend fun excludeFolderPath(folderPath: String, folderName: String) = withContext(Dispatchers.IO) {
        db.customizationAndCacheDao().upsertScanFolder(
            ScanFolderEntity(
                uriOrPath = folderPath,
                displayName = folderName,
                isTreeUri = false,
                isExcluded = true
            )
        )
        scanAll()
    }
}
