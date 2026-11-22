package com.example.data.tagger

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.example.core.model.EditableTrackTags
import com.example.core.util.AudioFormatSupport
import com.example.core.util.TextNormalizer
import com.example.data.db.CadenceDatabase
import com.example.data.db.TagEditHistoryEntity
import com.example.data.db.TrackEntity
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

data class ReadTagResult(
    val tags: EditableTrackTags,
    val rawPropertyMap: HashMap<String, Array<String>>,
    val pictures: Array<Picture>,
    val artworkWidth: Int = 0,
    val artworkHeight: Int = 0,
    val artworkMime: String = "",
    val artworkByteSize: Int = 0,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val channels: Int = 2,
    val lengthSec: Int = 0,
    val replayGainTrackDb: Float? = null,
    val replayGainAlbumDb: Float? = null
)

sealed class TagWriteOutcome {
    data class Success(val trackId: Long, val message: String) : TagWriteOutcome()
    data class NeedsUserPermission(val pendingIntent: PendingIntent, val trackId: Long) : TagWriteOutcome()
    data class Failure(val trackId: Long, val reason: String) : TagWriteOutcome()
}

data class BatchFieldSelection(
    val title: Boolean = false,
    val artist: Boolean = false,
    val album: Boolean = false,
    val albumArtist: Boolean = false,
    val genre: Boolean = false,
    val year: Boolean = false,
    val composer: Boolean = false,
    val conductor: Boolean = false,
    val comment: Boolean = false,
    val publisher: Boolean = false,
    val recordLabel: Boolean = false,
    val copyright: Boolean = false,
    val bpm: Boolean = false,
    val grouping: Boolean = false,
    val language: Boolean = false,
    val customTags: Boolean = false,
    val isCompilation: Boolean = false,
    val autoIncrementTrackNumber: Boolean = false,
    val startTrackNumber: Int = 1,
    val setTotalTracksToBatchSize: Boolean = false
)

class TagEditorEngine(
    private val context: Context,
    private val db: CadenceDatabase
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun readTagsFromTrack(track: TrackEntity): ReadTagResult? = withContext(Dispatchers.IO) {
        readTagsFromUri(Uri.parse(track.contentUri))
    }

    suspend fun readTagsFromUri(uri: Uri): ReadTagResult? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val meta = resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                TagLib.getMetadata(pfd.dup().detachFd(), true)
            }
            val props = resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                TagLib.getAudioProperties(pfd.dup().detachFd(), AudioPropertiesReadStyle.Average)
            }
            val map = meta?.propertyMap ?: HashMap()
            val pics = meta?.pictures ?: emptyArray()
            val front = pics.firstOrNull { it.pictureType.equals("Front Cover", ignoreCase = true) }
                ?: pics.firstOrNull()

            var artW = 0
            var artH = 0
            if (front != null && front.data.isNotEmpty()) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(front.data, 0, front.data.size, opts)
                artW = opts.outWidth
                artH = opts.outHeight
            }

            val trackPair = firstVal(map, "TRACKNUMBER")
            val (trkNum, trkTot) = splitSlashPair(trackPair, firstVal(map, "TOTALTRACKS"))
            val discPair = firstVal(map, "DISCNUMBER")
            val (dscNum, dscTot) = splitSlashPair(discPair, firstVal(map, "TOTALDISCS"))
            val rawDate = firstVal(map, "DATE", "YEAR")

            val tags = EditableTrackTags(
                title = firstVal(map, "TITLE"),
                artist = firstVal(map, "ARTIST"),
                album = firstVal(map, "ALBUM"),
                albumArtist = firstVal(map, "ALBUMARTIST"),
                trackNumber = trkNum,
                totalTracks = trkTot,
                discNumber = dscNum,
                totalDiscs = dscTot,
                year = rawDate.take(4).filter { it.isDigit() },
                releaseDate = rawDate,
                originalReleaseDate = firstVal(map, "ORIGINALDATE"),
                genre = firstVal(map, "GENRE"),
                composer = firstVal(map, "COMPOSER"),
                conductor = firstVal(map, "CONDUCTOR"),
                comment = firstVal(map, "COMMENT"),
                lyrics = firstVal(map, "LYRICS", "UNSYNCEDLYRICS"),
                copyright = firstVal(map, "COPYRIGHT"),
                publisher = firstVal(map, "PUBLISHER"),
                recordLabel = firstVal(map, "LABEL"),
                encoder = firstVal(map, "ENCODEDBY", "ENCODER"),
                bpm = firstVal(map, "BPM"),
                musicalKey = firstVal(map, "INITIALKEY", "KEY"),
                isrc = firstVal(map, "ISRC"),
                grouping = firstVal(map, "GROUPING"),
                subtitle = firstVal(map, "SUBTITLE"),
                language = firstVal(map, "LANGUAGE"),
                customTags = firstVal(map, "CADENCE_TAGS", "KEYWORDS"),
                isCompilation = firstVal(map, "COMPILATION") == "1" || firstVal(map, "COMPILATION").equals("true", true),
                rating = firstVal(map, "RATING").toIntOrNull()?.coerceIn(0, 5) ?: 0
            )

            ReadTagResult(
                tags = tags,
                rawPropertyMap = map,
                pictures = pics,
                artworkWidth = artW,
                artworkHeight = artH,
                artworkMime = front?.mimeType.orEmpty(),
                artworkByteSize = front?.data?.size ?: 0,
                bitrateKbps = props?.bitrate ?: 0,
                sampleRateHz = props?.sampleRate ?: 0,
                channels = props?.channels ?: 2,
                lengthSec = props?.length ?: 0,
                replayGainTrackDb = parseReplayGainDb(firstVal(map, "REPLAYGAIN_TRACK_GAIN")),
                replayGainAlbumDb = parseReplayGainDb(firstVal(map, "REPLAYGAIN_ALBUM_GAIN"))
            )
        }.getOrNull()
    }

    suspend fun writeSingleTrackTags(
        track: TrackEntity,
        newTags: EditableTrackTags,
        newArtworkBytes: ByteArray? = null,
        newArtworkMime: String = "image/jpeg",
        removeArtwork: Boolean = false
    ): TagWriteOutcome = withContext(Dispatchers.IO) {
        val uri = Uri.parse(track.contentUri)
        val currentRead = readTagsFromUri(uri)
        val beforeTags = currentRead?.tags ?: EditableTrackTags(
            title = track.title, artist = track.artist, album = track.album,
            albumArtist = track.albumArtist, genre = track.genre,
            year = track.year.takeIf { it > 0 }?.toString().orEmpty()
        )
        val mergedMap = HashMap<String, Array<String>>()
        currentRead?.rawPropertyMap?.forEach { (k, v) -> mergedMap[k] = v }

        applyTagsToPropertyMap(mergedMap, newTags)

        val targetPictures: Array<Picture>? = when {
            removeArtwork -> emptyArray()
            newArtworkBytes != null && newArtworkBytes.isNotEmpty() -> {
                arrayOf(Picture(newArtworkBytes, "Cover", "Front Cover", newArtworkMime))
            }
            else -> null // preserve existing artwork untouched
        }

        val (outcome, writtenSizeBytes) = performAtomicStageAndWrite(track, uri, mergedMap, targetPictures)
        if (outcome is TagWriteOutcome.Success) {
            db.customizationAndCacheDao().insertTagHistory(
                TagEditHistoryEntity(
                    trackId = track.id,
                    contentUri = track.contentUri,
                    beforeTagsJson = json.encodeToString(beforeTags),
                    afterTagsJson = json.encodeToString(newTags)
                )
            )
            val updated = applyTagsToEntity(
                track = track,
                tags = newTags,
                removeArtwork = removeArtwork,
                replacedArtwork = newArtworkBytes != null,
                newFileSizeBytes = writtenSizeBytes
            )
            db.trackDao().updateTrack(updated)
            com.example.ui.components.invalidateTrackArtworkCache(context, track.contentUri, track.artworkUri)
            if (track.filePath.isNotBlank()) {
                MediaScannerConnection.scanFile(context, arrayOf(track.filePath), null, null)
            }
        }
        outcome
    }

    suspend fun writeBatchTrackTags(
        tracks: List<TrackEntity>,
        templateTags: EditableTrackTags,
        selection: BatchFieldSelection,
        newArtworkBytes: ByteArray? = null,
        newArtworkMime: String = "image/jpeg",
        removeArtwork: Boolean = false
    ): List<TagWriteOutcome> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TagWriteOutcome>()
        tracks.forEachIndexed { idx, track ->
            val current = readTagsFromTrack(track)?.tags ?: EditableTrackTags(
                title = track.title, artist = track.artist, album = track.album,
                albumArtist = track.albumArtist, genre = track.genre,
                year = track.year.takeIf { it > 0 }?.toString().orEmpty()
            )
            val merged = current.copy(
                title = if (selection.title) templateTags.title else current.title,
                artist = if (selection.artist) templateTags.artist else current.artist,
                album = if (selection.album) templateTags.album else current.album,
                albumArtist = if (selection.albumArtist) templateTags.albumArtist else current.albumArtist,
                genre = if (selection.genre) templateTags.genre else current.genre,
                year = if (selection.year) templateTags.year else current.year,
                releaseDate = if (selection.year && current.releaseDate.length <= 4) templateTags.year else current.releaseDate,
                composer = if (selection.composer) templateTags.composer else current.composer,
                conductor = if (selection.conductor) templateTags.conductor else current.conductor,
                comment = if (selection.comment) templateTags.comment else current.comment,
                publisher = if (selection.publisher) templateTags.publisher else current.publisher,
                recordLabel = if (selection.recordLabel) templateTags.recordLabel else current.recordLabel,
                copyright = if (selection.copyright) templateTags.copyright else current.copyright,
                bpm = if (selection.bpm) templateTags.bpm else current.bpm,
                grouping = if (selection.grouping) templateTags.grouping else current.grouping,
                language = if (selection.language) templateTags.language else current.language,
                customTags = if (selection.customTags) templateTags.customTags else current.customTags.ifBlank { track.customTags },
                isCompilation = if (selection.isCompilation) templateTags.isCompilation else current.isCompilation,
                trackNumber = if (selection.autoIncrementTrackNumber) (selection.startTrackNumber + idx).toString() else current.trackNumber,
                totalTracks = if (selection.setTotalTracksToBatchSize) tracks.size.toString() else current.totalTracks
            )
            results.add(
                writeSingleTrackTags(
                    track = track,
                    newTags = merged,
                    newArtworkBytes = if (removeArtwork) null else newArtworkBytes,
                    newArtworkMime = newArtworkMime,
                    removeArtwork = removeArtwork
                )
            )
        }
        results
    }

    suspend fun rollbackTagEdit(history: TagEditHistoryEntity): TagWriteOutcome = withContext(Dispatchers.IO) {
        val track = db.trackDao().getTrackById(history.trackId)
            ?: return@withContext TagWriteOutcome.Failure(history.trackId, "Track is no longer in library.")
        val before = runCatching { json.decodeFromString<EditableTrackTags>(history.beforeTagsJson) }.getOrNull()
            ?: return@withContext TagWriteOutcome.Failure(history.trackId, "Rollback snapshot invalid.")
        val res = writeSingleTrackTags(track, before)
        if (res is TagWriteOutcome.Success) {
            db.customizationAndCacheDao().markTagHistoryRolledBack(history.id)
        }
        res
    }

    fun parseTagsFromFilename(fileNameWithExt: String, template: String): Map<String, String> {
        val base = fileNameWithExt.substringBeforeLast('.')
        // Convert template like "%artist% - %title%" into named regex
        val tokens = mutableListOf<String>()
        val regexSb = StringBuilder("^")
        var i = 0
        while (i < template.length) {
            if (template[i] == '%') {
                val end = template.indexOf('%', i + 1)
                if (end > i + 1) {
                    val token = template.substring(i + 1, end).lowercase()
                    tokens.add(token)
                    regexSb.append(if (token == "track" || token == "year" || token == "disc") "(\\d+)" else "(.+?)")
                    i = end + 1
                    continue
                }
            }
            regexSb.append(Regex.escape(template[i].toString()))
            i++
        }
        regexSb.append("$")
        val match = Regex(regexSb.toString()).matchEntire(base) ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        tokens.forEachIndexed { idx, key ->
            out[key] = match.groupValues.getOrNull(idx + 1)?.trim().orEmpty()
        }
        return out
    }

    fun formatFilenameFromTemplate(track: TrackEntity, template: String): String {
        val ext = track.fileName.substringAfterLast('.', track.format.lowercase())
        val trackPad = if (track.trackNumber > 0) track.trackNumber.toString().padStart(2, '0') else ""
        val discPad = if (track.discNumber > 0) track.discNumber.toString() else ""
        val replaced = template
            .replace("%artist%", TextNormalizer.sanitizeFilename(track.artist))
            .replace("%albumartist%", TextNormalizer.sanitizeFilename(track.albumArtist.ifBlank { track.artist }))
            .replace("%album%", TextNormalizer.sanitizeFilename(track.album))
            .replace("%title%", TextNormalizer.sanitizeFilename(track.title))
            .replace("%track%", trackPad)
            .replace("%disc%", discPad)
            .replace("%year%", track.year.takeIf { it > 0 }?.toString().orEmpty())
            .replace("%genre%", TextNormalizer.sanitizeFilename(track.genre))
        val leaf = TextNormalizer.sanitizeFilename(replaced.substringAfterLast('/'))
        return if (ext.isNotBlank()) "$leaf.$ext" else leaf
    }

    suspend fun renameTrackFile(track: TrackEntity, newFileName: String): Result<TrackEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = TextNormalizer.sanitizeFilename(newFileName.substringBeforeLast('.')) +
                "." + track.fileName.substringAfterLast('.', track.format.lowercase())
            val f = File(track.filePath)
            if (track.filePath.isNotBlank() && f.exists() && f.parentFile != null) {
                val target = File(f.parentFile, safeName)
                if (target.exists() && target.absolutePath != f.absolutePath) {
                    error("A file named '$safeName' already exists in this folder.")
                }
                if (f.renameTo(target)) {
                    MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath, target.absolutePath), null, null)
                    val updated = track.copy(
                        filePath = target.absolutePath,
                        fileName = target.name,
                        contentUri = Uri.fromFile(target).toString()
                    )
                    db.trackDao().updateTrack(updated)
                    return@runCatching updated
                }
            }
            error("Rename is not permitted on this storage provider without direct folder write access.")
        }
    }

    private fun performAtomicStageAndWrite(
        track: TrackEntity,
        uri: Uri,
        propertyMap: HashMap<String, Array<String>>,
        pictures: Array<Picture>?
    ): Pair<TagWriteOutcome, Long> {
        val stageDir = File(context.cacheDir, "tag_stage").apply { mkdirs() }
        val ext = track.fileName.substringAfterLast('.', "mp3").ifBlank { "mp3" }
        val tempFile = File(stageDir, "stage_${track.id}_${System.currentTimeMillis()}.$ext")

        return try {
            // 1. Copy source bytes to temp staging file
            val copied = context.contentResolver.openInputStream(uri)?.use { inp ->
                FileOutputStream(tempFile).use { out ->
                    inp.copyTo(out)
                    out.flush()
                }
                true
            } ?: run {
                val direct = track.filePath.takeIf { it.isNotBlank() }?.let { File(it) }
                if (direct != null && direct.exists() && direct.canRead()) {
                    direct.copyTo(tempFile, overwrite = true)
                    true
                } else {
                    false
                }
            }

            if (!copied || !tempFile.exists() || tempFile.length() == 0L) {
                return TagWriteOutcome.Failure(track.id, "Cannot read source audio file for staging.") to track.fileSizeBytes
            }

            // 2. Write tags on staged file via TagLib
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_WRITE).use { pfd ->
                val okTags = TagLib.savePropertyMap(pfd.dup().detachFd(), propertyMap)
                if (!okTags) {
                    return TagWriteOutcome.Failure(track.id, "TagLib rejected metadata format for ${track.format}.") to track.fileSizeBytes
                }
            }
            if (pictures != null) {
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_WRITE).use { pfd ->
                    val okPic = TagLib.savePictures(pfd.dup().detachFd(), pictures)
                    if (!okPic) {
                        return TagWriteOutcome.Failure(track.id, "Could not embed artwork into ${track.format}.") to track.fileSizeBytes
                    }
                }
            }

            // 3. Verify integrity of staged file before touching original
            val verifiedProps = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                TagLib.getAudioProperties(pfd.dup().detachFd(), AudioPropertiesReadStyle.Fast)
            }
            if (verifiedProps == null || (track.durationMs > 2000L && verifiedProps.length <= 0)) {
                return TagWriteOutcome.Failure(track.id, "Integrity check failed on staged output; original file preserved.") to track.fileSizeBytes
            }
            val finalSize = tempFile.length().coerceAtLeast(1L)

            // 4. Write staged bytes back to original file or ContentResolver
            val directFile = track.filePath.takeIf { it.isNotBlank() }?.let { File(it) }
            if (directFile != null && directFile.exists() && directFile.canWrite()) {
                val backupFile = File(stageDir, "bak_${track.id}.$ext")
                val directSucceeded = try {
                    directFile.copyTo(backupFile, overwrite = true)
                    FileOutputStream(directFile, false).use { out ->
                        tempFile.inputStream().use { inp -> inp.copyTo(out) }
                        out.fd.sync()
                    }
                    backupFile.delete()
                    true
                } catch (_: Exception) {
                    runCatching {
                        if (backupFile.exists()) backupFile.copyTo(directFile, overwrite = true)
                    }
                    runCatching { backupFile.delete() }
                    false
                }
                if (directSucceeded) {
                    return TagWriteOutcome.Success(track.id, "Tags written to file.") to finalSize
                }
                // Fall through to ContentResolver / MediaStore write if direct file write failed under scoped storage
            }

            // 5. Try ContentResolver write (SAF DocumentUri or granted MediaStore Uri)
            try {
                var wroteViaResolver = false
                // MediaStore Audio URIs work reliably with "rw" + truncate(0), while "wt" is often unsupported
                try {
                    context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            fos.channel.truncate(0L)
                            tempFile.inputStream().use { inp -> inp.copyTo(fos) }
                            fos.flush()
                        }
                        runCatching { pfd.fileDescriptor.sync() }
                        wroteViaResolver = true
                    }
                } catch (sec: SecurityException) {
                    throw sec
                } catch (_: Exception) {
                    // Fallback to output stream "w" or "wt" for SAF/other providers
                }

                if (!wroteViaResolver) {
                    val stream = runCatching { context.contentResolver.openOutputStream(uri, "w") }.getOrNull()
                        ?: runCatching { context.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
                    if (stream != null) {
                        stream.use { out ->
                            tempFile.inputStream().use { inp -> inp.copyTo(out) }
                            out.flush()
                        }
                        wroteViaResolver = true
                    }
                }

                if (!wroteViaResolver) {
                    return TagWriteOutcome.Failure(track.id, "Storage provider does not allow write stream.") to track.fileSizeBytes
                }
                TagWriteOutcome.Success(track.id, "Tags written to audio file.") to finalSize
            } catch (sec: SecurityException) {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri.scheme == ContentResolver.SCHEME_CONTENT -> {
                        val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                        TagWriteOutcome.NeedsUserPermission(pi, track.id) to track.fileSizeBytes
                    }
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && sec is android.app.RecoverableSecurityException -> {
                        TagWriteOutcome.NeedsUserPermission(sec.userAction.actionIntent, track.id) to track.fileSizeBytes
                    }
                    else -> {
                        TagWriteOutcome.Failure(track.id, "Write permission denied by storage provider.") to track.fileSizeBytes
                    }
                }
            }
        } catch (e: Exception) {
            TagWriteOutcome.Failure(track.id, e.localizedMessage ?: "Unexpected write error.") to track.fileSizeBytes
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private fun applyTagsToPropertyMap(map: HashMap<String, Array<String>>, tags: EditableTrackTags) {
        fun putOrRemove(key: String, value: String) {
            val v = value.trim()
            if (v.isEmpty()) map.remove(key) else map[key] = arrayOf(v)
        }
        putOrRemove("TITLE", tags.title)
        putOrRemove("ARTIST", tags.artist)
        putOrRemove("ALBUM", tags.album)
        putOrRemove("ALBUMARTIST", tags.albumArtist)
        val trackVal = if (tags.totalTracks.isNotBlank() && tags.trackNumber.isNotBlank()) {
            "${tags.trackNumber.trim()}/${tags.totalTracks.trim()}"
        } else tags.trackNumber
        putOrRemove("TRACKNUMBER", trackVal)
        putOrRemove("TOTALTRACKS", tags.totalTracks)
        val discVal = if (tags.totalDiscs.isNotBlank() && tags.discNumber.isNotBlank()) {
            "${tags.discNumber.trim()}/${tags.totalDiscs.trim()}"
        } else tags.discNumber
        putOrRemove("DISCNUMBER", discVal)
        putOrRemove("TOTALDISCS", tags.totalDiscs)
        val dateVal = tags.releaseDate.ifBlank { tags.year }
        putOrRemove("DATE", dateVal)
        if (map.containsKey("YEAR")) putOrRemove("YEAR", tags.year)
        putOrRemove("ORIGINALDATE", tags.originalReleaseDate)
        putOrRemove("GENRE", tags.genre)
        putOrRemove("COMPOSER", tags.composer)
        putOrRemove("CONDUCTOR", tags.conductor)
        putOrRemove("COMMENT", tags.comment)
        putOrRemove("LYRICS", tags.lyrics)
        putOrRemove("COPYRIGHT", tags.copyright)
        putOrRemove("PUBLISHER", tags.publisher)
        putOrRemove("LABEL", tags.recordLabel)
        putOrRemove("ENCODEDBY", tags.encoder)
        putOrRemove("BPM", tags.bpm)
        putOrRemove("INITIALKEY", tags.musicalKey)
        putOrRemove("ISRC", tags.isrc)
        putOrRemove("GROUPING", tags.grouping)
        putOrRemove("SUBTITLE", tags.subtitle)
        putOrRemove("LANGUAGE", tags.language)
        putOrRemove("CADENCE_TAGS", tags.customTags)
        if (tags.isCompilation) map["COMPILATION"] = arrayOf("1") else map.remove("COMPILATION")
        if (tags.rating > 0) map["RATING"] = arrayOf(tags.rating.coerceIn(1, 5).toString()) else map.remove("RATING")
    }

    private fun applyTagsToEntity(
        track: TrackEntity,
        tags: EditableTrackTags,
        removeArtwork: Boolean,
        replacedArtwork: Boolean,
        newFileSizeBytes: Long
    ): TrackEntity {
        val title = tags.title.ifBlank { track.title }
        val artist = tags.artist.ifBlank { "Unknown Artist" }
        val album = tags.album.ifBlank { "Unknown Album" }
        val year = (tags.year.toIntOrNull() ?: tags.releaseDate.take(4).toIntOrNull() ?: 0)
        return track.copy(
            title = title,
            artist = artist,
            album = album,
            albumArtist = tags.albumArtist,
            genre = tags.genre,
            composer = tags.composer,
            conductor = tags.conductor,
            comment = tags.comment,
            copyright = tags.copyright,
            publisher = tags.publisher,
            recordLabel = tags.recordLabel,
            encoder = tags.encoder,
            grouping = tags.grouping,
            subtitle = tags.subtitle,
            language = tags.language,
            customTags = tags.customTags.trim(),
            isrc = tags.isrc,
            musicalKey = tags.musicalKey,
            releaseDate = tags.releaseDate,
            originalReleaseDate = tags.originalReleaseDate,
            year = year,
            trackNumber = tags.trackNumber.toIntOrNull() ?: 0,
            totalTracks = tags.totalTracks.toIntOrNull() ?: 0,
            discNumber = tags.discNumber.toIntOrNull() ?: 0,
            totalDiscs = tags.totalDiscs.toIntOrNull() ?: 0,
            bpm = tags.bpm.toIntOrNull() ?: 0,
            isCompilation = tags.isCompilation,
            rating = tags.rating,
            fileSizeBytes = newFileSizeBytes,
            dateModifiedSec = System.currentTimeMillis() / 1000L,
            hasLyrics = tags.lyrics.isNotBlank() || track.hasLyrics,
            hasEmbeddedArtwork = when {
                removeArtwork -> false
                replacedArtwork -> true
                else -> track.hasEmbeddedArtwork
            },
            artworkUri = if (removeArtwork) null else track.artworkUri,
            normalizedSearch = TextNormalizer.buildTrackSearchIndex(
                title, artist, album, tags.albumArtist, tags.genre, tags.composer, track.folderName
            )
        )
    }

    private fun firstVal(map: Map<String, Array<String>>, vararg keys: String): String {
        for (k in keys) {
            val v = map[k]?.firstOrNull()?.trim()
            if (!v.isNullOrEmpty()) return v
        }
        return ""
    }

    private fun splitSlashPair(pairValue: String, fallbackTotal: String): Pair<String, String> {
        if (pairValue.contains("/")) {
            val parts = pairValue.split("/", limit = 2)
            return parts[0].trim() to parts[1].trim().ifBlank { fallbackTotal }
        }
        return pairValue.trim() to fallbackTotal.trim()
    }

    private fun parseReplayGainDb(raw: String): Float? {
        if (raw.isBlank()) return null
        return raw.lowercase().replace("db", "").trim().toFloatOrNull()
    }
}
