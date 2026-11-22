package com.example.data.metadata

import com.example.core.model.MetadataCandidate
import com.example.core.model.ProviderId
import com.example.core.util.TextNormalizer
import com.example.data.db.MetadataCacheDaoHelper
import com.example.data.db.TrackEntity
import com.example.data.preferences.AppSettingsSnapshot
import com.example.data.preferences.SettingsRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// --- iTunes Search API ---
@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesTrackItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItunesTrackItem(
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val collectionArtistName: String? = null,
    val primaryGenreName: String? = null,
    val releaseDate: String? = null,
    val trackNumber: Int? = null,
    val trackCount: Int? = null,
    val discNumber: Int? = null,
    val trackTimeMillis: Long? = null,
    val artworkUrl100: String? = null
)

interface ItunesApi {
    @GET("search")
    suspend fun searchTracks(
        @Query("term") term: String,
        @Query("country") country: String = "US",
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 15
    ): Response<ItunesSearchResponse>

    companion object {
        const val BASE_URL = "https://itunes.apple.com/"
    }
}

// --- Deezer Public Search API ---
@JsonClass(generateAdapter = true)
data class DeezerSearchResponse(
    val data: List<DeezerTrackItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeezerTrackItem(
    val id: Long? = null,
    val title: String? = null,
    val duration: Int? = null,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbum? = null
)

@JsonClass(generateAdapter = true)
data class DeezerArtist(val id: Long? = null, val name: String? = null)

@JsonClass(generateAdapter = true)
data class DeezerAlbum(
    val id: Long? = null,
    val title: String? = null,
    @Json(name = "cover_xl") val coverXl: String? = null,
    @Json(name = "cover_big") val coverBig: String? = null
)

interface DeezerApi {
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 12
    ): Response<DeezerSearchResponse>

    companion object {
        const val BASE_URL = "https://api.deezer.com/"
    }
}

// --- MusicBrainz v2 API ---
@JsonClass(generateAdapter = true)
data class MusicBrainzSearchResponse(
    val recordings: List<MusicBrainzRecording> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MusicBrainzRecording(
    val id: String = "",
    val score: Int? = null,
    val title: String? = null,
    val length: Long? = null,
    @Json(name = "artist-credit") val artistCredit: List<MusicBrainzArtistCredit>? = null,
    val releases: List<MusicBrainzRelease>? = null,
    val isrcs: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class MusicBrainzArtistCredit(
    val name: String? = null,
    val artist: MusicBrainzArtist? = null
)

@JsonClass(generateAdapter = true)
data class MusicBrainzArtist(val name: String? = null)

@JsonClass(generateAdapter = true)
data class MusicBrainzRelease(
    val id: String? = null,
    val title: String? = null,
    val date: String? = null,
    @Json(name = "track-count") val trackCount: Int? = null,
    @Json(name = "cover-art-archive") val coverArtArchive: CoverArtArchiveStatus? = null
)

@JsonClass(generateAdapter = true)
data class CoverArtArchiveStatus(
    val artwork: Boolean? = null,
    val front: Boolean? = null
)

interface MusicBrainzApi {
    @GET("ws/2/recording")
    suspend fun searchRecordings(
        @Query("query") luceneQuery: String,
        @Query("inc") inc: String = "releases+artist-credits+isrcs",
        @Query("fmt") fmt: String = "json",
        @Query("limit") limit: Int = 10,
        @Header("User-Agent") userAgent: String = "CadenceMusicPlayer/1.0.0 (cadence-player.local)"
    ): Response<MusicBrainzSearchResponse>

    companion object {
        const val BASE_URL = "https://musicbrainz.org/"
    }
}

// --- Discogs Database Search API ---
@JsonClass(generateAdapter = true)
data class DiscogsSearchResponse(
    val results: List<DiscogsResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DiscogsResult(
    val id: Long? = null,
    val title: String? = null,
    val year: String? = null,
    val genre: List<String>? = null,
    val label: List<String>? = null,
    @Json(name = "cover_image") val coverImage: String? = null
)

interface DiscogsApi {
    @GET("database/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "release",
        @Query("per_page") perPage: Int = 8,
        @Header("Authorization") authHeader: String,
        @Header("User-Agent") userAgent: String = "CadenceMusicPlayer/1.0.0"
    ): Response<DiscogsSearchResponse>

    companion object {
        const val BASE_URL = "https://api.discogs.com/"
    }
}


sealed class MetadataLookupResult {
    data class Success(
        val candidates: List<MetadataCandidate>,
        val providerNotices: List<String> = emptyList()
    ) : MetadataLookupResult()

    data class OfflineOrDisabled(val message: String) : MetadataLookupResult()
}

class MetadataRepository(
    private val settingsRepository: SettingsRepository,
    private val cacheHelper: MetadataCacheDaoHelper,
    private val okHttpClient: OkHttpClient
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val mbMutex = Mutex()
    private var lastMbRequestAt = 0L

    private val itunesApi: ItunesApi by lazy {
        Retrofit.Builder().baseUrl(ItunesApi.BASE_URL).client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(ItunesApi::class.java)
    }
    private val deezerApi: DeezerApi by lazy {
        Retrofit.Builder().baseUrl(DeezerApi.BASE_URL).client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(DeezerApi::class.java)
    }
    private val musicBrainzApi: MusicBrainzApi by lazy {
        Retrofit.Builder().baseUrl(MusicBrainzApi.BASE_URL).client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(MusicBrainzApi::class.java)
    }
    private val discogsApi: DiscogsApi by lazy {
        Retrofit.Builder().baseUrl(DiscogsApi.BASE_URL).client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(DiscogsApi::class.java)
    }

    suspend fun searchMetadata(
        track: TrackEntity,
        settings: AppSettingsSnapshot,
        customQuery: String? = null
    ): MetadataLookupResult = withContext(Dispatchers.IO) {
        if (!settings.networkLookupsEnabled) {
            return@withContext MetadataLookupResult.OfflineOrDisabled("Network metadata lookups are disabled in Privacy settings.")
        }
        val queryText = customQuery?.takeIf { it.isNotBlank() }
            ?: listOf(track.artist, track.title)
                .filter { it.isNotBlank() && it != "Unknown Artist" }
                .joinToString(" ")
                .ifBlank { track.fileName }

        val candidates = mutableListOf<MetadataCandidate>()
        val notices = mutableListOf<String>()

        for (provider in settings.metadataProviderPriority) {
            when (provider) {
                ProviderId.ITUNES -> candidates.addAll(queryItunesUs(track, queryText))
                ProviderId.DEEZER -> candidates.addAll(queryDeezer(track, queryText))
                ProviderId.MUSICBRAINZ -> candidates.addAll(queryMusicBrainz(track, queryText))
                ProviderId.DISCOGS -> {
                    val token = settingsRepository.resolveDiscogsToken(settings)
                    if (token.isNotBlank()) {
                        candidates.addAll(queryDiscogs(track, queryText, token))
                    }
                }
                ProviderId.ACOUSTID -> {
                    val key = settingsRepository.resolveAcoustIdKey(settings)
                    if (key.isNotBlank()) {
                        notices.add("AcoustID key active; acoustic fingerprint lookup requires Chromaprint fingerprint.")
                    }
                }
                else -> Unit
            }
        }

        val ranked = candidates.sortedByDescending { it.confidenceScore }
        MetadataLookupResult.Success(ranked, notices)
    }

    private suspend fun queryItunesUs(track: TrackEntity, query: String): List<MetadataCandidate> {
        val cacheKey = "itunes_us:${TextNormalizer.normalizeForSearch(query)}"
        readCachedList(cacheKey)?.let { return it }

        val list = runCatching {
            val resp = itunesApi.searchTracks(term = query, country = "US", media = "music", entity = "song", limit = 15)
            if (!resp.isSuccessful) return emptyList()
            resp.body()?.results.orEmpty().map { item ->
                val highResArt = item.artworkUrl100?.replace(Regex("\\d+x\\d+bb"), "1200x1200bb")
                val year = item.releaseDate?.take(4)?.toIntOrNull()
                val title = item.trackName.orEmpty()
                val artist = item.artistName.orEmpty()
                MetadataCandidate(
                    provider = ProviderId.ITUNES,
                    externalId = (item.trackId ?: 0L).toString(),
                    title = title,
                    artist = artist,
                    album = item.collectionName,
                    albumArtist = item.collectionArtistName ?: item.artistName,
                    genre = item.primaryGenreName,
                    releaseDate = item.releaseDate?.take(10),
                    year = year,
                    trackNumber = item.trackNumber,
                    totalTracks = item.trackCount,
                    discNumber = item.discNumber,
                    durationMs = item.trackTimeMillis,
                    artworkUrl = highResArt,
                    confidenceScore = scoreMatch(track, title, artist, item.collectionName, item.trackTimeMillis, null)
                )
            }
        }.getOrDefault(emptyList())

        if (list.isNotEmpty()) writeCachedList(cacheKey, ProviderId.ITUNES, list)
        return list
    }

    private suspend fun queryDeezer(track: TrackEntity, query: String): List<MetadataCandidate> {
        val cacheKey = "deezer:${TextNormalizer.normalizeForSearch(query)}"
        readCachedList(cacheKey)?.let { return it }

        val list = runCatching {
            val resp = deezerApi.searchTracks(query = query)
            if (!resp.isSuccessful) return emptyList()
            resp.body()?.data.orEmpty().map { item ->
                val title = item.title.orEmpty()
                val artist = item.artist?.name.orEmpty()
                val album = item.album?.title
                val durMs = item.duration?.let { it * 1000L }
                MetadataCandidate(
                    provider = ProviderId.DEEZER,
                    externalId = (item.id ?: 0L).toString(),
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = artist,
                    durationMs = durMs,
                    artworkUrl = item.album?.coverXl ?: item.album?.coverBig,
                    confidenceScore = scoreMatch(track, title, artist, album, durMs, null)
                )
            }
        }.getOrDefault(emptyList())

        if (list.isNotEmpty()) writeCachedList(cacheKey, ProviderId.DEEZER, list)
        return list
    }

    private suspend fun queryMusicBrainz(track: TrackEntity, query: String): List<MetadataCandidate> {
        val cacheKey = "mb:${TextNormalizer.normalizeForSearch(query)}"
        readCachedList(cacheKey)?.let { return it }

        // Respect MusicBrainz 1 req/sec rate limit
        mbMutex.withLock {
            val elapsed = System.currentTimeMillis() - lastMbRequestAt
            if (elapsed < 1050L) delay(1050L - elapsed)
            lastMbRequestAt = System.currentTimeMillis()
        }

        val lucene = if (track.title.isNotBlank() && track.artist.isNotBlank() && track.artist != "Unknown Artist") {
            "recording:\"${track.title.replace("\"", "")}\" AND artist:\"${track.artist.replace("\"", "")}\""
        } else {
            query
        }

        val list = runCatching {
            val resp = musicBrainzApi.searchRecordings(luceneQuery = lucene)
            if (!resp.isSuccessful) return emptyList()
            resp.body()?.recordings.orEmpty().map { rec ->
                val artist = rec.artistCredit?.joinToString("") { (it.name ?: it.artist?.name).orEmpty() }.orEmpty()
                val release = rec.releases?.firstOrNull()
                val caaFront = if (release?.id != null && release.coverArtArchive?.front == true) {
                    "https://coverartarchive.org/release/${release.id}/front"
                } else null
                val isrc = rec.isrcs?.firstOrNull()
                val title = rec.title.orEmpty()
                MetadataCandidate(
                    provider = ProviderId.MUSICBRAINZ,
                    externalId = rec.id,
                    title = title,
                    artist = artist,
                    album = release?.title,
                    releaseDate = release?.date,
                    year = release?.date?.take(4)?.toIntOrNull(),
                    totalTracks = release?.trackCount,
                    durationMs = rec.length,
                    isrc = isrc,
                    artworkUrl = caaFront,
                    confidenceScore = scoreMatch(track, title, artist, release?.title, rec.length, isrc)
                )
            }
        }.getOrDefault(emptyList())

        if (list.isNotEmpty()) writeCachedList(cacheKey, ProviderId.MUSICBRAINZ, list)
        return list
    }

    private suspend fun queryDiscogs(track: TrackEntity, query: String, token: String): List<MetadataCandidate> {
        return runCatching {
            val resp = discogsApi.search(query = query, authHeader = "Discogs token=$token")
            if (!resp.isSuccessful) return emptyList()
            resp.body()?.results.orEmpty().map { r ->
                val parts = r.title.orEmpty().split(" - ", limit = 2)
                val artist = if (parts.size == 2) parts[0].trim() else track.artist
                val albumTitle = if (parts.size == 2) parts[1].trim() else r.title.orEmpty()
                MetadataCandidate(
                    provider = ProviderId.DISCOGS,
                    externalId = (r.id ?: 0L).toString(),
                    title = track.title,
                    artist = artist,
                    album = albumTitle,
                    year = r.year?.toIntOrNull(),
                    genre = r.genre?.firstOrNull(),
                    recordLabel = r.label?.firstOrNull(),
                    artworkUrl = r.coverImage,
                    confidenceScore = scoreMatch(track, track.title, artist, albumTitle, null, null) * 0.9f
                )
            }
        }.getOrDefault(emptyList())
    }


    suspend fun downloadFullResArtworkBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).header("User-Agent", LrcLibApiUserAgent).build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.bytes()
            }
        }.getOrNull()
    }

    private fun scoreMatch(
        track: TrackEntity,
        candTitle: String,
        candArtist: String,
        candAlbum: String?,
        candDurationMs: Long?,
        candIsrc: String?
    ): Float {
        if (!candIsrc.isNullOrBlank() && candIsrc.equals(track.isrc, ignoreCase = true)) {
            return 0.99f
        }
        val titleSim = TextNormalizer.similarity(track.title, candTitle)
        val artistSim = if (track.artist.isNotBlank() && track.artist != "Unknown Artist") {
            TextNormalizer.similarity(track.artist, candArtist)
        } else 0.6f
        val albumSim = if (!candAlbum.isNullOrBlank() && track.album.isNotBlank() && track.album != "Unknown Album") {
            TextNormalizer.similarity(track.album, candAlbum)
        } else 0.5f
        val durScore = if (candDurationMs != null && track.durationMs > 0) {
            val diffSec = abs(candDurationMs - track.durationMs) / 1000.0
            (1.0 - (diffSec / 18.0)).coerceIn(0.0, 1.0).toFloat()
        } else 0.65f
        return ((titleSim * 0.45f) + (artistSim * 0.30f) + (albumSim * 0.10f) + (durScore * 0.15f)).coerceIn(0f, 1f)
    }

    private suspend fun readCachedList(key: String): List<MetadataCandidate>? {
        val raw = cacheHelper.getFreshJson(key, maxAgeMs = TimeUnit.DAYS.toMillis(7)) ?: return null
        val type = Types.newParameterizedType(List::class.java, MetadataCandidate::class.java)
        return runCatching { moshi.adapter<List<MetadataCandidate>>(type).fromJson(raw) }.getOrNull()
    }

    private suspend fun writeCachedList(key: String, provider: ProviderId, items: List<MetadataCandidate>) {
        val type = Types.newParameterizedType(List::class.java, MetadataCandidate::class.java)
        val json = moshi.adapter<List<MetadataCandidate>>(type).toJson(items)
        cacheHelper.putJson(key, provider.name, json)
    }

    companion object {
        private const val LrcLibApiUserAgent = "CadenceMusicPlayer/1.0.0 (cadence-player.local)"
    }
}
