package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.CadenceApplication
import com.example.core.model.GeneratedSmartMix
import com.example.core.model.LibraryFilter
import com.example.core.model.LyricsStorageMode
import com.example.core.model.RatingSystemMode
import com.example.core.model.SmartPlaylistDefinition
import com.example.core.model.SmartViewCategory
import com.example.core.model.SortDirection
import com.example.core.model.SortField
import com.example.core.model.ThemeConfig
import com.example.widget.CadenceWidgetUpdater
import com.example.core.util.TextNormalizer
import com.example.data.db.AlbumSummary
import com.example.data.db.ArtistSummary
import com.example.data.db.CustomThemeEntity
import com.example.data.db.EqualizerPresetEntity
import com.example.data.db.FolderSummary
import com.example.data.db.GenreSummary
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistWithCount
import com.example.data.db.SmartPlaylistEntity
import com.example.data.db.TrackEntity
import com.example.data.db.YearSummary
import com.example.data.preferences.AppSettingsSnapshot
import com.example.data.preferences.ScreenSortPreference
import com.example.data.smart.SmartPlaylistEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class HomeShelvesState(
    val continueTracks: List<TrackEntity> = emptyList(),
    val recentPlayed: List<TrackEntity> = emptyList(),
    val recentAdded: List<TrackEntity> = emptyList(),
    val mostPlayed: List<TrackEntity> = emptyList(),
    val favorites: List<TrackEntity> = emptyList(),
    val forgottenGems: List<TrackEntity> = emptyList(),
    val localDiscoveryMix: List<TrackEntity> = emptyList(),
    val unfinishedAlbums: List<AlbumSummary> = emptyList()
)

data class SearchMatchesState(
    val artists: List<ArtistSummary> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val playlists: List<PlaylistWithCount> = emptyList(),
    val folders: List<FolderSummary> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val container = (application as CadenceApplication).container
    private val db = container.database
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val settings: StateFlow<AppSettingsSnapshot> = container.settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsSnapshot())

    val playbackState = container.playerConnection.uiState
    val playbackProgress = container.playerConnection.progressState
    val lyricsSyncState = container.lyricsSyncEngine.state
    val scanState = container.libraryScanner.scanState
    val eqState = container.audioEffectsController.state
    val playbackErrors = container.playbackDiagnostics.errors
    val lastPlaybackBanner = container.playbackDiagnostics.lastErrorMessage

    val allTracks: StateFlow<List<TrackEntity>> = db.trackDao().observeAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumSummary>> = db.trackDao().observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<ArtistSummary>> = db.trackDao().observeArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumArtists: StateFlow<List<ArtistSummary>> = db.trackDao().observeAlbumArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<GenreSummary>> = db.trackDao().observeGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val composers: StateFlow<List<ArtistSummary>> = db.trackDao().observeComposers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val years: StateFlow<List<YearSummary>> = db.trackDao().observeYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderSummary>> = db.trackDao().observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistWithCount>> = db.playlistDao().observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartPlaylists: StateFlow<List<SmartPlaylistEntity>> = db.playlistDao().observeSmartPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queueItems = db.queueDao().observeQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queueHistory = db.queueDao().observeQueueHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryStats = db.trackDao().observeLibraryStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyStats = db.historyAndStatsDao().observeDailyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customThemes: StateFlow<List<CustomThemeEntity>> = db.customizationAndCacheDao().observeCustomThemes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEqPresets: StateFlow<List<EqualizerPresetEntity>> = db.customizationAndCacheDao().observeEqPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanFolders = db.customizationAndCacheDao().observeScanFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory = db.customizationAndCacheDao().observeSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tagEditHistory = db.customizationAndCacheDao().observeTagHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Personalized Auto-Generated Smart Mixes based on listening habits, artists, genres, and user tags/moods
    val personalizedMixes: StateFlow<List<GeneratedSmartMix>> = combine(
        allTracks,
        settings.map { it.ratingSystemMode }.distinctUntilChanged()
    ) { tracks, ratingMode ->
        if (tracks.isEmpty()) {
            emptyList()
        } else {
            val mixes = mutableListOf<GeneratedSmartMix>()
            fun TrackEntity.score(): Double {
                val dur = durationMs.coerceAtLeast(1L).toDouble()
                val approxPlays = (playCount + skipCount).coerceAtLeast(1)
                val completion = (totalListenMs.toDouble() / (dur * approxPlays)).coerceIn(0.0, 1.5)
                val favBonus = if (ratingMode == RatingSystemMode.STARS) rating * 2.5 else if (isFavorite) 6.0 else 0.0
                return (playCount * 4.0) + (completion * 4.0) + favBonus - (skipCount * 2.0)
            }

            // 1. Heavy Rotation / Top Mix
            val heavy = tracks.sortedByDescending { it.score() }.take(25)
            if (heavy.isNotEmpty()) {
                val lead = heavy.first()
                mixes += GeneratedSmartMix(
                    id = "heavy_rotation",
                    title = "Heavy Rotation",
                    subtitle = "${heavy.size} tracks • Top listens & high completion",
                    badge = "FOR YOU",
                    artworkUri = heavy.firstNotNullOfOrNull { it.artworkUri },
                    fallbackContentUri = lead.contentUri,
                    trackIds = heavy.map { it.id }
                )
            }

            // 2. Top Artists (up to 2)
            val artistGroups = tracks
                .filter { it.artist.isNotBlank() && !it.artist.equals("Unknown Artist", ignoreCase = true) }
                .groupBy { it.artist.trim() }
                .values
                .filter { it.size >= 2 }
                .sortedByDescending { list ->
                    list.sumOf { it.playCount * 5L + (it.totalListenMs / 15000L) + it.rating * 2L + if (it.isFavorite) 3L else 0L } + list.size
                }
                .take(2)

            artistGroups.forEachIndexed { idx, group ->
                val artistName = group.first().artist.trim()
                val sorted = group.sortedByDescending { it.score() }.take(25)
                mixes += GeneratedSmartMix(
                    id = "artist_mix_$idx",
                    title = "$artistName Essentials",
                    subtitle = "${sorted.size} tracks • Artist focus",
                    badge = "ARTIST MIX",
                    artworkUri = sorted.firstNotNullOfOrNull { it.artworkUri },
                    fallbackContentUri = sorted.first().contentUri,
                    trackIds = sorted.map { it.id }
                )
            }

            // 3. User-Defined Mood / Custom Tag Mixes (up to 2)
            val tagBuckets = linkedMapOf<String, MutableList<TrackEntity>>()
            tracks.forEach { track ->
                track.customTags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinctBy { it.lowercase() }
                    .forEach { tag ->
                        val key = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        tagBuckets.getOrPut(key) { mutableListOf() }.add(track)
                    }
            }
            tagBuckets.entries
                .filter { it.value.size >= 2 }
                .sortedByDescending { entry -> entry.value.sumOf { it.playCount + 1 } }
                .take(2)
                .forEach { (moodName, moodTracks) ->
                    val sorted = moodTracks.sortedByDescending { it.score() }.take(25)
                    mixes += GeneratedSmartMix(
                        id = "mood_${moodName.lowercase().replace(Regex("[^a-z0-9]"), "_")}",
                        title = "$moodName Mood",
                        subtitle = "${sorted.size} tracks • From your user tags",
                        badge = "MOOD MIX",
                        artworkUri = sorted.firstNotNullOfOrNull { it.artworkUri },
                        fallbackContentUri = sorted.first().contentUri,
                        trackIds = sorted.map { it.id }
                    )
                }

            // 4. Top Genre Station
            val topGenre = tracks
                .filter { it.genre.isNotBlank() && !it.genre.equals("Unknown Genre", ignoreCase = true) }
                .groupBy { it.genre.trim() }
                .entries
                .filter { it.value.size >= 2 }
                .maxByOrNull { (_, list) -> list.sumOf { it.playCount * 3 + 1 } }
            if (topGenre != null) {
                val sorted = topGenre.value.sortedByDescending { it.score() }.take(25)
                mixes += GeneratedSmartMix(
                    id = "genre_top",
                    title = "${topGenre.key} Station",
                    subtitle = "${sorted.size} tracks • Genre rotation",
                    badge = "GENRE",
                    artworkUri = sorted.firstNotNullOfOrNull { it.artworkUri },
                    fallbackContentUri = sorted.first().contentUri,
                    trackIds = sorted.map { it.id }
                )
            }

            // 5. Rediscover / Forgotten Gems
            val rediscover = tracks
                .filter { it.playCount == 0 || (it.skipCount == 0 && it.lastPlayedAt > 0L) }
                .sortedWith(
                    compareByDescending<TrackEntity> {
                        if (ratingMode == RatingSystemMode.STARS) it.rating else if (it.isFavorite) 5 else 0
                    }.thenBy { it.playCount }.thenByDescending { it.dateAddedSec }
                )
                .take(20)
            if (rediscover.isNotEmpty()) {
                mixes += GeneratedSmartMix(
                    id = "rediscover_gems",
                    title = "Rediscover Gems",
                    subtitle = "${rediscover.size} tracks • Rarely played & top picks",
                    badge = "REDISCOVER",
                    artworkUri = rediscover.firstNotNullOfOrNull { it.artworkUri },
                    fallbackContentUri = rediscover.first().contentUri,
                    trackIds = rediscover.map { it.id }
                )
            }

            mixes
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Background-computed Home shelves (never blocks Main thread on library updates)
    val homeShelves: StateFlow<HomeShelvesState> = combine(
        allTracks,
        albums,
        settings.map { it.ratingSystemMode }.distinctUntilChanged()
    ) { tracks, albumList, ratingMode ->
        if (tracks.isEmpty()) {
            HomeShelvesState()
        } else {
            val playedAlbumKeys = tracks.asSequence().filter { it.playCount > 0 }.map { it.album }.toHashSet()
            val scoredHighlights = if (ratingMode == RatingSystemMode.STARS) {
                tracks.filter { it.rating >= 4 }
                    .sortedWith(compareByDescending<TrackEntity> { it.rating }.thenByDescending { it.playCount })
                    .take(15)
            } else {
                tracks.filter { it.isFavorite }.take(15)
            }
            HomeShelvesState(
                continueTracks = tracks.filter { it.lastResumePosMs > 15_000L && it.durationMs > 120_000L }
                    .sortedByDescending { it.lastPlayedAt }
                    .take(10),
                recentPlayed = tracks.filter { it.lastPlayedAt > 0L }
                    .sortedByDescending { it.lastPlayedAt }
                    .take(15),
                recentAdded = tracks.sortedByDescending { it.dateAddedSec }.take(15),
                mostPlayed = tracks.filter { it.playCount > 0 }
                    .sortedByDescending { it.playCount }
                    .take(15),
                favorites = scoredHighlights,
                forgottenGems = tracks.filter { it.playCount == 0 || (it.rating >= 4 && it.lastPlayedAt == 0L) }
                    .take(15),
                localDiscoveryMix = tracks.sortedByDescending {
                    (it.rating * 3) + (if (it.isFavorite) 4 else 0) + (it.playCount.coerceAtMost(10)) - (it.skipCount * 2)
                }.take(20),
                unfinishedAlbums = albumList.filter { it.album in playedAlbumKeys && it.trackCount > 1 }.take(10)
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeShelvesState())

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<TrackEntity>> = _searchQuery
        .debounce(120)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { trimmed ->
            if (trimmed.isEmpty()) {
                MutableStateFlow(emptyList())
            } else {
                db.trackDao().searchTracks(TextNormalizer.normalizeForSearch(trimmed), trimmed)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchCollectionMatches: StateFlow<SearchMatchesState> = combine(
        _searchQuery.debounce(120).map { TextNormalizer.normalizeForSearch(it.trim()) }.distinctUntilChanged(),
        artists,
        albums,
        playlists,
        folders
    ) { normQuery, artistList, albumList, playlistList, folderList ->
        if (normQuery.isBlank()) {
            SearchMatchesState()
        } else {
            SearchMatchesState(
                artists = artistList.filter { TextNormalizer.normalizeForSearch(it.artist).contains(normQuery) }.take(8),
                albums = albumList.filter {
                    TextNormalizer.normalizeForSearch(it.album).contains(normQuery) ||
                        TextNormalizer.normalizeForSearch(it.albumArtist).contains(normQuery)
                }.take(8),
                playlists = playlistList.filter { TextNormalizer.normalizeForSearch(it.name).contains(normQuery) }.take(6),
                folders = folderList.filter {
                    TextNormalizer.normalizeForSearch(it.folderName).contains(normQuery) ||
                        TextNormalizer.normalizeForSearch(it.folderPath).contains(normQuery)
                }.take(6)
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchMatchesState())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun commitSearchToHistory(q: String) {
        val clean = q.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            db.customizationAndCacheDao().upsertSearchHistory(
                com.example.data.db.SearchHistoryEntity(query = clean)
            )
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            db.customizationAndCacheDao().clearSearchHistory()
        }
    }

    fun triggerLibraryScan(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val res = container.libraryScanner.scanAll(forceRefresh)
            res.onSuccess { count ->
                showMessage("Indexed $count audio files.")
            }.onFailure { err ->
                showMessage("Scan error: ${err.localizedMessage}")
            }
        }
    }

    fun observeTracksForScreen(screenKey: String, extraWhere: String = "", extraArgs: List<Any> = emptyList()): Flow<List<TrackEntity>> {
        return settings
            .map { it.screenSortPrefs[screenKey] ?: ScreenSortPreference() }
            .distinctUntilChanged()
            .flatMapLatest { pref ->
                db.trackDao().observeTracksRaw(
                    SmartPlaylistEvaluator.buildLibraryQuery(
                        sortField = pref.field,
                        sortDirection = pref.direction,
                        filter = pref.filter,
                        extraWhere = extraWhere,
                        extraArgs = extraArgs
                    )
                )
            }
    }

    suspend fun getTrackById(trackId: Long): TrackEntity? = withContext(Dispatchers.IO) {
        db.trackDao().getTrackById(trackId)
    }

    fun observeMixTracks(mixId: String): Flow<List<TrackEntity>> {
        return combine(personalizedMixes, allTracks) { mixes, tracks ->
            val mix = mixes.firstOrNull { it.id == mixId } ?: return@combine emptyList()
            val map = tracks.associateBy { it.id }
            mix.trackIds.mapNotNull { map[it] }
        }.flowOn(Dispatchers.Default)
    }

    fun saveGeneratedMixAsPlaylist(mix: GeneratedSmartMix) {
        if (mix.trackIds.isEmpty()) return
        createPlaylist(name = mix.title, description = mix.subtitle, initialTrackIds = mix.trackIds)
    }

    fun observeAlbumsForArtist(artistName: String, isAlbumArtist: Boolean): Flow<List<AlbumSummary>> {
        val where = if (isAlbumArtist) {
            "LOWER(TRIM(COALESCE(NULLIF(albumArtist, ''), artist))) = LOWER(TRIM(?))"
        } else {
            "(LOWER(TRIM(artist)) = LOWER(TRIM(?)) OR LOWER(TRIM(albumArtist)) = LOWER(TRIM(?)))"
        }
        val args = if (isAlbumArtist) listOf(artistName) else listOf(artistName, artistName)
        return db.trackDao().observeTracksRaw(
            SmartPlaylistEvaluator.buildLibraryQuery(
                SortField.YEAR, SortDirection.DESC, LibraryFilter(),
                where, args
            )
        ).map { tracks ->
            tracks.groupBy { it.album to (it.albumArtist.ifBlank { it.artist }) }
                .map { (key, list) ->
                    AlbumSummary(
                        album = key.first,
                        albumArtist = key.second,
                        albumId = list.maxOfOrNull { it.albumId } ?: 0L,
                        artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                        trackCount = list.size,
                        totalDurationMs = list.sumOf { it.durationMs },
                        year = list.maxOfOrNull { it.year } ?: 0
                    )
                }
                .sortedWith(compareByDescending<AlbumSummary> { it.year }.thenBy { it.album.lowercase() })
        }.flowOn(Dispatchers.Default)
    }

    fun observeSmartPlaylistTracks(smartPlaylistId: Long): Flow<List<TrackEntity>> {
        return smartPlaylists.map { list ->
            list.firstOrNull { it.id == smartPlaylistId }
        }.flatMapLatest { entity ->
            if (entity == null) {
                MutableStateFlow(emptyList())
            } else {
                val def = SmartPlaylistEvaluator.parseDefinition(entity.definitionJson)
                db.trackDao().observeTracksRaw(SmartPlaylistEvaluator.buildSmartPlaylistQuery(def))
            }
        }
    }

    fun observeSmartViewCategory(category: SmartViewCategory): Flow<List<TrackEntity>> {
        val nowSec = System.currentTimeMillis() / 1000L
        return when (category) {
            SmartViewCategory.RECENTLY_ADDED -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.DATE_ADDED, SortDirection.DESC, LibraryFilter(),
                    "dateAddedSec >= ?", listOf(nowSec - 30L * 86400L)
                )
            )
            SmartViewCategory.RECENTLY_PLAYED -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.LAST_PLAYED, SortDirection.DESC, LibraryFilter(),
                    "lastPlayedAt > 0"
                )
            )
            SmartViewCategory.MOST_PLAYED -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.PLAY_COUNT, SortDirection.DESC, LibraryFilter(),
                    "playCount > 0"
                )
            )
            SmartViewCategory.LEAST_PLAYED -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.PLAY_COUNT, SortDirection.ASC, LibraryFilter()
                )
            )
            SmartViewCategory.FREQUENTLY_SKIPPED -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.PLAY_COUNT, SortDirection.ASC, LibraryFilter(),
                    "skipCount > 0 AND skipCount >= playCount"
                )
            )
            SmartViewCategory.FAVORITES -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TITLE, SortDirection.ASC, LibraryFilter(),
                    "isFavorite = 1"
                )
            )
            SmartViewCategory.UNRATED -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TITLE, SortDirection.ASC, LibraryFilter(),
                    "rating = 0"
                )
            )
            SmartViewCategory.MISSING_ARTWORK -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TITLE, SortDirection.ASC, LibraryFilter(),
                    "hasEmbeddedArtwork = 0 AND artworkUri IS NULL"
                )
            )
            SmartViewCategory.MISSING_LYRICS -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TITLE, SortDirection.ASC, LibraryFilter(),
                    "hasLyrics = 0"
                )
            )
            SmartViewCategory.MISSING_METADATA -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TITLE, SortDirection.ASC, LibraryFilter(),
                    "artist = 'Unknown Artist' OR album = 'Unknown Album' OR TRIM(genre) = '' OR year = 0"
                )
            )
            SmartViewCategory.DUPLICATES -> allTracks.map { tracks ->
                tracks.groupBy {
                    "${TextNormalizer.normalizeForSearch(it.title)}::${TextNormalizer.normalizeForSearch(it.artist)}::${it.durationMs / 2000L}"
                }.values.filter { it.size > 1 }.flatten()
            }
            SmartViewCategory.LONGEST -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.DURATION, SortDirection.DESC, LibraryFilter()
                )
            )
            SmartViewCategory.SHORTEST -> db.trackDao().observeTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.DURATION, SortDirection.ASC, LibraryFilter()
                )
            )
        }
    }

    fun updateScreenSort(screenKey: String, pref: ScreenSortPreference) {
        viewModelScope.launch {
            container.settingsRepository.updateScreenSort(screenKey, pref)
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        val next = !track.isFavorite
        viewModelScope.launch(Dispatchers.IO) {
            db.trackDao().setFavorite(track.id, next)
            container.playerConnection.refreshCurrentTrackFromDb()
            CadenceWidgetUpdater.updateFavoriteState(getApplication(), track.id, next)
        }
    }

    fun setTrackRating(trackId: Long, rating: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.trackDao().setRating(trackId, rating.coerceIn(0, 5))
            container.playerConnection.refreshCurrentTrackFromDb()
        }
    }

    fun updateTrackCustomTags(trackIds: List<Long>, customTagsCsv: String) {
        if (trackIds.isEmpty()) return
        val cleaned = customTagsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .joinToString(", ")
        viewModelScope.launch(Dispatchers.IO) {
            trackIds.chunked(200).forEach { chunk ->
                db.trackDao().updateCustomTagsBatch(chunk, cleaned)
            }
            container.playerConnection.refreshCurrentTrackFromDb()
            showMessage("Updated user tags on ${trackIds.size} track(s).")
        }
    }

    // Playlist Management
    fun createPlaylist(name: String, description: String = "", initialTrackIds: List<Long> = emptyList()) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = db.playlistDao().insertPlaylist(PlaylistEntity(name = clean, description = description))
            if (initialTrackIds.isNotEmpty()) {
                db.playlistDao().replacePlaylistTracks(id, initialTrackIds)
            }
            showMessage("Created playlist \"$clean\".")
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.playlistDao().getAllPlaylistsOnce().firstOrNull { it.id == playlistId } ?: return@launch
            db.playlistDao().updatePlaylist(existing.copy(name = clean, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.playlistDao().deletePlaylist(playlistId)
        }
    }

    fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.playlistDao().getPlaylistTracksOnce(playlistId).map { it.id }
            val merged = existing + trackIds
            db.playlistDao().replacePlaylistTracks(playlistId, merged)
            showMessage("Added ${trackIds.size} track(s) to playlist.")
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.playlistDao().getPlaylistTracksOnce(playlistId).map { it.id }.toMutableList()
            if (index in existing.indices) {
                existing.removeAt(index)
                db.playlistDao().replacePlaylistTracks(playlistId, existing)
            }
        }
    }

    fun removeTracksFromPlaylist(playlistId: Long, trackIds: Set<Long>) {
        if (trackIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.playlistDao().getPlaylistTracksOnce(playlistId).map { it.id }
            val filtered = existing.filter { it !in trackIds }
            db.playlistDao().replacePlaylistTracks(playlistId, filtered)
            showMessage("Removed ${existing.size - filtered.size} track(s) from playlist.")
        }
    }

    fun deletePlaylists(playlistIds: Set<Long>) {
        if (playlistIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            playlistIds.forEach { db.playlistDao().deletePlaylist(it) }
            showMessage("Deleted ${playlistIds.size} playlist(s).")
        }
    }

    suspend fun getTracksForAlbum(albumName: String, albumArtist: String): List<TrackEntity> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            db.trackDao().queryTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TRACK_NUMBER, SortDirection.ASC, LibraryFilter(),
                    "album = ? AND COALESCE(NULLIF(albumArtist, ''), artist) = ?",
                    listOf(albumName, albumArtist)
                )
            )
        }

    suspend fun getTracksForArtist(artistName: String, isAlbumArtist: Boolean): List<TrackEntity> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val where = if (isAlbumArtist) {
                "LOWER(TRIM(COALESCE(NULLIF(albumArtist, ''), artist))) = LOWER(TRIM(?))"
            } else {
                "(LOWER(TRIM(artist)) = LOWER(TRIM(?)) OR LOWER(TRIM(albumArtist)) = LOWER(TRIM(?)))"
            }
            val args = if (isAlbumArtist) listOf(artistName) else listOf(artistName, artistName)
            db.trackDao().queryTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.ALBUM, SortDirection.ASC, LibraryFilter(),
                    where, args
                )
            )
        }

    suspend fun getTracksForGenre(genreName: String): List<TrackEntity> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            db.trackDao().queryTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TITLE, SortDirection.ASC, LibraryFilter(),
                    "COALESCE(NULLIF(genre, ''), 'Unknown Genre') = ?", listOf(genreName)
                )
            )
        }

    suspend fun getTracksForYear(year: Int): List<TrackEntity> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            db.trackDao().queryTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.ALBUM, SortDirection.ASC, LibraryFilter(),
                    "year = ?", listOf(year)
                )
            )
        }

    suspend fun getTracksForFolder(folderPath: String): List<TrackEntity> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            db.trackDao().queryTracksRaw(
                SmartPlaylistEvaluator.buildLibraryQuery(
                    SortField.TITLE, SortDirection.ASC, LibraryFilter(),
                    "folderPath = ?", listOf(folderPath)
                )
            )
        }

    fun excludeFolder(folderPath: String, folderName: String) {
        if (folderPath.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            container.libraryScanner.excludeFolderPath(folderPath, folderName)
            showMessage("Excluded folder \"$folderName\".")
        }
    }

    fun duplicatePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val pl = db.playlistDao().getAllPlaylistsOnce().firstOrNull { it.id == playlistId } ?: return@launch
            val tracks = db.playlistDao().getPlaylistTracksOnce(playlistId).map { it.id }
            val newId = db.playlistDao().insertPlaylist(PlaylistEntity(name = "${pl.name} (Copy)", description = pl.description))
            db.playlistDao().replacePlaylistTracks(newId, tracks)
            showMessage("Duplicated \"${pl.name}\".")
        }
    }

    fun mergePlaylists(sourceId: Long, targetId: Long) {
        if (sourceId == targetId) return
        viewModelScope.launch(Dispatchers.IO) {
            val targetTracks = db.playlistDao().getPlaylistTracksOnce(targetId).map { it.id }
            val sourceTracks = db.playlistDao().getPlaylistTracksOnce(sourceId).map { it.id }
            db.playlistDao().replacePlaylistTracks(targetId, (targetTracks + sourceTracks).distinct())
            showMessage("Merged playlists.")
        }
    }

    fun saveQueueAsPlaylist(name: String) {
        val clean = name.trim().ifBlank { "Saved Queue" }
        viewModelScope.launch(Dispatchers.IO) {
            val ids = db.queueDao().getQueueTrackIdsOnce()
            if (ids.isEmpty()) {
                showMessage("Queue is empty.")
                return@launch
            }
            val id = db.playlistDao().insertPlaylist(PlaylistEntity(name = clean))
            db.playlistDao().replacePlaylistTracks(id, ids)
            showMessage("Saved queue as \"$clean\".")
        }
    }

    fun saveSmartPlaylist(entity: SmartPlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.playlistDao().upsertSmartPlaylist(entity.copy(updatedAt = System.currentTimeMillis()))
            showMessage("Saved smart playlist \"${entity.name}\".")
        }
    }

    fun deleteSmartPlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.playlistDao().deleteSmartPlaylist(id)
        }
    }

    // Bookmarks
    fun observeBookmarks(trackId: Long) = db.historyAndStatsDao().observeBookmarks(trackId)

    fun addBookmark(trackId: Long, label: String, positionMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.historyAndStatsDao().insertBookmark(
                com.example.data.db.BookmarkEntity(
                    trackId = trackId,
                    label = label.ifBlank { "Bookmark" },
                    positionMs = positionMs
                )
            )
            showMessage("Saved bookmark at ${TextNormalizer.formatDuration(positionMs)}")
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.historyAndStatsDao().deleteBookmark(id)
        }
    }

    // Themes
    fun updateThemeConfig(transform: (ThemeConfig) -> ThemeConfig) {
        viewModelScope.launch { container.settingsRepository.updateThemeConfig(transform) }
    }

    fun saveCustomTheme(name: String, config: ThemeConfig) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            db.customizationAndCacheDao().upsertCustomTheme(
                CustomThemeEntity(name = clean, configJson = json.encodeToString(config))
            )
            showMessage("Saved custom theme \"$clean\".")
        }
    }

    fun deleteCustomTheme(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.customizationAndCacheDao().deleteCustomTheme(id)
        }
    }

    // Share intents
    fun shareTracks(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val ctx = getApplication<Application>()
        val uris = tracks.mapNotNull { t ->
            runCatching {
                if (t.filePath.isNotBlank() && File(t.filePath).exists()) {
                    FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(t.filePath))
                } else {
                    Uri.parse(t.contentUri)
                }
            }.getOrNull()
        }
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share Audio").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(chooser)
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            container.libraryScanner.deleteTrackFile(track)
                .onSuccess { showMessage("Deleted \"${track.title}\".") }
                .onFailure { showMessage(it.localizedMessage ?: "Delete failed.") }
        }
    }

    fun deleteTracks(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            var deleted = 0
            tracks.forEach { t ->
                if (container.libraryScanner.deleteTrackFile(t).isSuccess) deleted++
            }
            showMessage("Deleted $deleted/${tracks.size} track(s).")
        }
    }

    fun resetStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            db.trackDao().resetListeningStatistics()
            db.historyAndStatsDao().clearPlaybackHistory()
            showMessage("Listening statistics reset.")
        }
    }
}
