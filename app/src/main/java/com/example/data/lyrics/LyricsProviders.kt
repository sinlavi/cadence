package com.example.data.lyrics

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.core.model.LyricsCandidate
import com.example.core.model.LyricsStorageMode
import com.example.core.model.ParsedLrc
import com.example.core.model.ProviderId
import com.example.core.util.TextNormalizer
import com.example.data.db.LyricsDao
import com.example.data.db.LyricsEntity
import com.example.data.db.MetadataCacheDaoHelper
import com.example.data.db.TrackDao
import com.example.data.db.TrackEntity
import com.example.data.preferences.AppSettingsSnapshot
import com.example.data.preferences.SettingsRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@JsonClass(generateAdapter = true)
data class LrcLibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

interface LrcLibApi {
    @GET("api/get")
    suspend fun getExact(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") durationSec: Int? = null,
        @Header("User-Agent") userAgent: String = USER_AGENT
    ): Response<LrcLibResponse>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("track_name") trackName: String? = null,
        @Query("artist_name") artistName: String? = null,
        @Query("album_name") albumName: String? = null,
        @Header("User-Agent") userAgent: String = USER_AGENT
    ): Response<List<LrcLibResponse>>

    companion object {
        const val USER_AGENT = "CadenceMusicPlayer/1.0.0 (https://cadence-player.local)"
        const val BASE_URL = "https://lrclib.net/"
    }
}

interface MusixmatchApi {
    @GET("ws/1.1/matcher.lyrics.get")
    suspend fun matchLyrics(
        @Query("apikey") apiKey: String,
        @Query("q_track") track: String,
        @Query("q_artist") artist: String
    ): Response<Map<String, Any?>>

    companion object {
        const val BASE_URL = "https://api.musixmatch.com/"
    }
}

sealed class LyricsLookupResult {
    data class Success(val candidates: List<LyricsCandidate>, val warnings: List<String> = emptyList()) : LyricsLookupResult()
    data class OfflineOrDisabled(val message: String) : LyricsLookupResult()
    data class Error(val message: String) : LyricsLookupResult()
}

class LyricsRepository(
    private val context: Context,
    private val lyricsDao: LyricsDao,
    private val trackDao: TrackDao,
    private val settingsRepository: SettingsRepository,
    private val cacheHelper: MetadataCacheDaoHelper,
    private val okHttpClient: OkHttpClient
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val lrcLibApi: LrcLibApi by lazy {
        Retrofit.Builder()
            .baseUrl(LrcLibApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LrcLibApi::class.java)
    }

    private val musixmatchApi: MusixmatchApi by lazy {
        Retrofit.Builder()
            .baseUrl(MusixmatchApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MusixmatchApi::class.java)
    }

    fun observeLyricsForTrack(trackId: Long): Flow<LyricsEntity?> = lyricsDao.observeLyricsForTrack(trackId)

    suspend fun loadBestLocalLyrics(track: TrackEntity, embeddedLyricsText: String?): LyricsEntity? = withContext(Dispatchers.IO) {
        val byId = lyricsDao.getLyricsForTrack(track.id)
        if (byId != null && (byId.syncedLrc.isNotBlank() || byId.plainLyrics.isNotBlank())) {
            return@withContext byId
        }
        // Check sidecar .lrc next to file path if readable
        val sidecarLrc = readSidecarLrc(track)
        if (!sidecarLrc.isNullOrBlank()) {
            val parsed = LrcParser.parse(sidecarLrc)
            val entity = LyricsEntity(
                trackId = track.id,
                fallbackKey = TextNormalizer.fallbackLyricsKey(track.title, track.artist),
                plainLyrics = parsed.lines.joinToString("\n") { it.text },
                syncedLrc = if (parsed.isSynced) sidecarLrc else "",
                isSynced = parsed.isSynced,
                source = "external_lrc",
                storageMode = LyricsStorageMode.EXTERNAL_LRC.name
            )
            lyricsDao.upsertLyrics(entity)
            trackDao.setHasLyrics(track.id, true)
            return@withContext entity
        }
        // Check embedded lyrics
        if (!embeddedLyricsText.isNullOrBlank()) {
            val parsed = LrcParser.parse(embeddedLyricsText)
            val entity = LyricsEntity(
                trackId = track.id,
                fallbackKey = TextNormalizer.fallbackLyricsKey(track.title, track.artist),
                plainLyrics = if (parsed.isSynced) parsed.lines.joinToString("\n") { it.text } else embeddedLyricsText,
                syncedLrc = if (parsed.isSynced) embeddedLyricsText else "",
                isSynced = parsed.isSynced,
                source = "embedded",
                storageMode = LyricsStorageMode.EMBEDDED_TAG.name
            )
            lyricsDao.upsertLyrics(entity)
            trackDao.setHasLyrics(track.id, true)
            return@withContext entity
        }
        // Fallback match by normalized title+artist in case file was moved
        val fallbackKey = TextNormalizer.fallbackLyricsKey(track.title, track.artist)
        val byFallback = lyricsDao.getLyricsByFallbackKey(fallbackKey)
        if (byFallback != null) {
            val relinked = byFallback.copy(id = 0, trackId = track.id)
            lyricsDao.upsertLyrics(relinked)
            trackDao.setHasLyrics(track.id, true)
            return@withContext relinked
        }
        null
    }

    suspend fun searchProviders(
        track: TrackEntity,
        settings: AppSettingsSnapshot,
        customQuery: String? = null
    ): LyricsLookupResult = withContext(Dispatchers.IO) {
        if (!settings.networkLookupsEnabled) {
            return@withContext LyricsLookupResult.OfflineOrDisabled("Online lyrics lookups are disabled in Privacy settings.")
        }
        val candidates = mutableListOf<LyricsCandidate>()
        val warnings = mutableListOf<String>()

        for (provider in settings.lyricsProviderPriority) {
            when (provider) {
                ProviderId.LRCLIB -> {
                    val found = fetchFromLrcLib(track, customQuery)
                    candidates.addAll(found)
                }
                ProviderId.MUSIXMATCH -> {
                    val key = settingsRepository.resolveMusixmatchKey(settings)
                    if (key.isNotBlank()) {
                        val mm = fetchFromMusixmatch(track, key)
                        if (mm != null) candidates.add(mm)
                    }
                }
                else -> Unit
            }
        }

        val sorted = candidates.distinctBy { "${it.provider}:${it.externalId}" }
            .sortedByDescending { it.confidenceScore }
        LyricsLookupResult.Success(sorted, warnings)
    }

    private suspend fun fetchFromLrcLib(track: TrackEntity, customQuery: String?): List<LyricsCandidate> {
        val results = mutableListOf<LyricsCandidate>()
        val durationSec = (track.durationMs / 1000L).toInt().takeIf { it > 0 }
        val cacheKey = "lrclib:${customQuery ?: "${track.artist}::${track.title}::${durationSec ?: 0}"}"

        val cached = cacheHelper.getFreshJson(cacheKey, maxAgeMs = TimeUnit.DAYS.toMillis(7))
        if (cached != null) {
            val adapter = moshi.adapter<List<LyricsCandidate>>(
                com.squareup.moshi.Types.newParameterizedType(List::class.java, LyricsCandidate::class.java)
            )
            runCatching { adapter.fromJson(cached) }.getOrNull()?.let { return it }
        }

        if (customQuery.isNullOrBlank()) {
            runCatching {
                val exact = lrcLibApi.getExact(
                    trackName = track.title,
                    artistName = track.artist,
                    albumName = track.album.takeIf { it.isNotBlank() && it != "Unknown Album" },
                    durationSec = durationSec
                )
                if (exact.isSuccessful) {
                    exact.body()?.toCandidate(track)?.let { results.add(it) }
                }
            }
        }

        runCatching {
            val searchResp = if (!customQuery.isNullOrBlank()) {
                lrcLibApi.search(query = customQuery)
            } else {
                lrcLibApi.search(
                    trackName = track.title,
                    artistName = track.artist
                )
            }
            if (searchResp.isSuccessful) {
                searchResp.body().orEmpty().forEach { item ->
                    results.add(item.toCandidate(track))
                }
            }
        }

        if (results.isNotEmpty()) {
            val adapter = moshi.adapter<List<LyricsCandidate>>(
                com.squareup.moshi.Types.newParameterizedType(List::class.java, LyricsCandidate::class.java)
            )
            cacheHelper.putJson(cacheKey, ProviderId.LRCLIB.name, adapter.toJson(results))
        }
        return results
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun fetchFromMusixmatch(track: TrackEntity, apiKey: String): LyricsCandidate? {
        return runCatching {
            val resp = musixmatchApi.matchLyrics(apiKey, track.title, track.artist)
            if (!resp.isSuccessful) return null
            val message = resp.body()?.get("message") as? Map<String, Any?> ?: return null
            val body = message["body"] as? Map<String, Any?> ?: return null
            val lyrics = body["lyrics"] as? Map<String, Any?> ?: return null
            val lyricsBody = (lyrics["lyrics_body"] as? String)?.trim().orEmpty()
            if (lyricsBody.isBlank()) return null
            LyricsCandidate(
                provider = ProviderId.MUSIXMATCH,
                externalId = (lyrics["lyrics_id"] ?: track.id).toString(),
                trackName = track.title,
                artistName = track.artist,
                plainLyrics = lyricsBody,
                syncedLyrics = null,
                confidenceScore = 0.78f
            )
        }.getOrNull()
    }

    private fun LrcLibResponse.toCandidate(track: TrackEntity): LyricsCandidate {
        val tScore = TextNormalizer.similarity(track.title, trackName.orEmpty())
        val aScore = TextNormalizer.similarity(track.artist, artistName.orEmpty())
        val trackSec = track.durationMs / 1000.0
        val durScore = if (duration != null && trackSec > 0) {
            val diff = abs(trackSec - duration)
            (1.0 - (diff / 15.0)).coerceIn(0.0, 1.0).toFloat()
        } else 0.7f
        val syncBonus = if (!syncedLyrics.isNullOrBlank()) 0.08f else 0f
        val score = ((tScore * 0.45f) + (aScore * 0.35f) + (durScore * 0.20f) + syncBonus).coerceIn(0f, 1f)
        return LyricsCandidate(
            provider = ProviderId.LRCLIB,
            externalId = (id ?: 0L).toString(),
            trackName = trackName ?: track.title,
            artistName = artistName ?: track.artist,
            albumName = albumName,
            durationSec = duration?.toInt(),
            instrumental = instrumental == true,
            plainLyrics = plainLyrics,
            syncedLyrics = syncedLyrics,
            confidenceScore = score
        )
    }

    suspend fun saveLyrics(
        track: TrackEntity,
        plainLyrics: String,
        syncedLrc: String,
        source: String,
        storageMode: LyricsStorageMode,
        userEdited: Boolean,
        writeEmbeddedCallback: (suspend (TrackEntity, String) -> Boolean)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val hasSynced = syncedLrc.isNotBlank() && LrcParser.parse(syncedLrc).isSynced
        val finalPlain = if (plainLyrics.isBlank() && hasSynced) {
            LrcParser.parse(syncedLrc).lines.joinToString("\n") { it.text }
        } else {
            plainLyrics
        }
        val contentToExport = if (hasSynced) syncedLrc else finalPlain
        var externalUri: String? = null

        if (storageMode == LyricsStorageMode.EXTERNAL_LRC && track.filePath.isNotBlank()) {
            val lrcFile = sidecarLrcFile(track.filePath)
            if (lrcFile != null) {
                runCatching {
                    lrcFile.writeText(contentToExport, Charsets.UTF_8)
                    externalUri = Uri.fromFile(lrcFile).toString()
                }
            }
        } else if (storageMode == LyricsStorageMode.EMBEDDED_TAG && writeEmbeddedCallback != null) {
            writeEmbeddedCallback(track, contentToExport)
        }

        val entry = LyricsEntity(
            trackId = track.id,
            fallbackKey = TextNormalizer.fallbackLyricsKey(track.title, track.artist),
            plainLyrics = finalPlain,
            syncedLrc = if (hasSynced) syncedLrc else "",
            isSynced = hasSynced,
            isInstrumental = false,
            source = source,
            storageMode = storageMode.name,
            externalFileUri = externalUri,
            userEdited = userEdited,
            updatedAt = System.currentTimeMillis()
        )
        lyricsDao.upsertLyrics(entry)
        trackDao.setHasLyrics(track.id, finalPlain.isNotBlank() || hasSynced)
        true
    }

    suspend fun importLrcFromUri(uri: Uri): ParsedLrc? = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@runCatching null
            LrcParser.parse(text)
        }.getOrNull()
    }

    suspend fun exportLrcToUri(uri: Uri, lrcContent: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                it.write(lrcContent)
            }
            true
        }.getOrDefault(false)
    }

    private fun readSidecarLrc(track: TrackEntity): String? {
        if (track.filePath.isBlank()) return null
        val file = sidecarLrcFile(track.filePath) ?: return null
        return if (file.exists() && file.canRead()) {
            runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
        } else null
    }

    private fun sidecarLrcFile(audioPath: String): File? {
        val f = File(audioPath)
        val parent = f.parentFile ?: return null
        val base = f.nameWithoutExtension
        return File(parent, "$base.lrc")
    }
}
