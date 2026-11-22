package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.HomeSectionKey
import com.example.core.model.LibraryFilter
import com.example.core.model.LyricsStorageMode
import com.example.core.model.NavDestinationKey
import com.example.core.model.ProviderId
import com.example.core.model.RatingSystemMode
import com.example.core.model.RepeatModeOption
import com.example.core.model.SortDirection
import com.example.core.model.SortField
import com.example.core.model.ThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cadence_settings")

@Serializable
data class ScreenSortPreference(
    val field: SortField = SortField.TITLE,
    val direction: SortDirection = SortDirection.ASC,
    val filter: LibraryFilter = LibraryFilter()
)

@Serializable
data class AppSettingsSnapshot(
    val themeConfig: ThemeConfig = ThemeConfig(),
    val ratingSystemMode: RatingSystemMode = RatingSystemMode.HEARTS,
    val navOrder: List<NavDestinationKey> = defaultNavOrder(),
    val hiddenNavItems: Set<NavDestinationKey> = defaultHiddenNavItems(),
    val homeSectionOrder: List<HomeSectionKey> = HomeSectionKey.entries,
    val hiddenHomeSections: Set<HomeSectionKey> = emptySet(),
    val homeShowGeneratedMixes: Boolean = true,
    val miniPlayerShowArtwork: Boolean = true,
    val miniPlayerShowPrevious: Boolean = false,
    val miniPlayerShowPlayPause: Boolean = true,
    val miniPlayerShowNext: Boolean = true,
    val miniPlayerShowFavorite: Boolean = true,
    val miniPlayerShowQueue: Boolean = false,
    val notificationShowPrevious: Boolean = true,
    val notificationShowNext: Boolean = true,
    val notificationShowFavorite: Boolean = true,
    val notificationShowSeekBack: Boolean = false,
    val notificationShowSeekForward: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val crossfadeSeconds: Float = 0f,
    val fadeOnPauseResume: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val skipSilence: Boolean = false,
    val replayGainEnabled: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val seekIntervalSec: Int = 10,
    val resumeTrackPosition: Boolean = true,
    val autoRemovePlayedFromQueue: Boolean = false,
    val preventQueueDuplicates: Boolean = false,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val duckOnAudioFocusLoss: Boolean = true,
    val autoScanOnLaunch: Boolean = false,
    val includeHiddenFolders: Boolean = false,
    val respectNomedia: Boolean = true,
    val minTrackDurationSec: Int = 3,
    val enabledFormats: Set<String> = setOf("MP3", "FLAC", "M4A", "AAC", "OGG", "OPUS", "WAV", "ALAC", "AIFF", "WMA", "APE"),
    val lyricsAutoLookup: Boolean = true,
    val lyricsAutoSave: Boolean = true,
    val lyricsStorageMode: LyricsStorageMode = LyricsStorageMode.APP_DATABASE,
    val lyricsFontSizeSp: Float = 18f,
    val lyricsLineSpacingMultiplier: Float = 1.45f,
    val lyricsAutoScroll: Boolean = true,
    val metadataProviderPriority: List<ProviderId> = listOf(
        ProviderId.ITUNES,
        ProviderId.DEEZER,
        ProviderId.MUSICBRAINZ,
        ProviderId.DISCOGS,
        ProviderId.ACOUSTID
    ),
    val lyricsProviderPriority: List<ProviderId> = listOf(
        ProviderId.LRCLIB,
        ProviderId.MUSIXMATCH
    ),
    val autoMatchConfidenceThreshold: Float = 0.72f,
    val networkLookupsEnabled: Boolean = true,
    val listeningStatsEnabled: Boolean = true,
    val equalizerEnabled: Boolean = false,
    val equalizerSelectedPreset: String = "Flat",
    val equalizerBandsCsv: String = "",
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val lastPlayedTrackId: Long = -1L,
    val lastPlayedPositionMs: Long = 0L,
    val screenSortPrefs: Map<String, ScreenSortPreference> = emptyMap(),
    val appleMusicTokenOverride: String = "",
    val discogsTokenOverride: String = "",
    val acoustIdKeyOverride: String = "",
    val musixmatchKeyOverride: String = ""
) {
    companion object {
        fun defaultNavOrder(): List<NavDestinationKey> = listOf(
            NavDestinationKey.HOME,
            NavDestinationKey.ARTISTS,
            NavDestinationKey.ALBUMS,
            NavDestinationKey.FILES,
            NavDestinationKey.PLAYLISTS,
            NavDestinationKey.SONGS,
            NavDestinationKey.GENRES,
            NavDestinationKey.YEARS,
            NavDestinationKey.COMPOSERS,
            NavDestinationKey.SMART_VIEWS,
            NavDestinationKey.LIBRARY,
            NavDestinationKey.SEARCH,
            NavDestinationKey.SETTINGS
        )

        fun defaultHiddenNavItems(): Set<NavDestinationKey> = setOf(
            NavDestinationKey.SONGS,
            NavDestinationKey.GENRES,
            NavDestinationKey.YEARS,
            NavDestinationKey.COMPOSERS,
            NavDestinationKey.SMART_VIEWS,
            NavDestinationKey.LIBRARY,
            NavDestinationKey.SEARCH,
            NavDestinationKey.SETTINGS
        )
    }
}

class SettingsRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val THEME_JSON = stringPreferencesKey("theme_config_json")
        val SETTINGS_JSON = stringPreferencesKey("app_settings_json")
        val LAST_TRACK_ID = longPreferencesKey("last_track_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val SHUFFLE = booleanPreferencesKey("shuffle_enabled")
        val REPEAT = stringPreferencesKey("repeat_mode")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val LOUDNESS = intPreferencesKey("loudness_mb")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
    }

    val settingsFlow: Flow<AppSettingsSnapshot> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SETTINGS_JSON]
        val base = if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<AppSettingsSnapshot>(raw) }
                .getOrDefault(AppSettingsSnapshot())
        } else {
            AppSettingsSnapshot()
        }
        val theme = prefs[Keys.THEME_JSON]?.let {
            runCatching { json.decodeFromString<ThemeConfig>(it) }.getOrNull()
        } ?: base.themeConfig

        val hasNewNavKeys = NavDestinationKey.ARTISTS in base.navOrder &&
            NavDestinationKey.ALBUMS in base.navOrder &&
            NavDestinationKey.FILES in base.navOrder

        val normalizedNavOrder = if (hasNewNavKeys) {
            (base.navOrder + NavDestinationKey.entries.filter { it !in base.navOrder }).distinct()
        } else {
            AppSettingsSnapshot.defaultNavOrder()
        }
        val normalizedHiddenNav = if (hasNewNavKeys) {
            base.hiddenNavItems + NavDestinationKey.SETTINGS
        } else {
            AppSettingsSnapshot.defaultHiddenNavItems()
        }

        base.copy(
            themeConfig = theme,
            navOrder = normalizedNavOrder,
            hiddenNavItems = normalizedHiddenNav,
            metadataProviderPriority = base.metadataProviderPriority
                .filter { it != ProviderId.APPLE_MUSIC }
                .ifEmpty { listOf(ProviderId.ITUNES, ProviderId.DEEZER, ProviderId.MUSICBRAINZ) },
            lyricsProviderPriority = base.lyricsProviderPriority
                .filter { it != ProviderId.APPLE_MUSIC }
                .ifEmpty { listOf(ProviderId.LRCLIB, ProviderId.MUSIXMATCH) },
            lastPlayedTrackId = prefs[Keys.LAST_TRACK_ID] ?: base.lastPlayedTrackId,
            lastPlayedPositionMs = prefs[Keys.LAST_POSITION_MS] ?: base.lastPlayedPositionMs,
            shuffleEnabled = prefs[Keys.SHUFFLE] ?: base.shuffleEnabled,
            repeatMode = prefs[Keys.REPEAT]?.let {
                runCatching { RepeatModeOption.valueOf(it) }.getOrNull()
            } ?: base.repeatMode,
            equalizerEnabled = prefs[Keys.EQ_ENABLED] ?: base.equalizerEnabled,
            equalizerSelectedPreset = prefs[Keys.EQ_PRESET] ?: base.equalizerSelectedPreset,
            equalizerBandsCsv = prefs[Keys.EQ_BANDS] ?: base.equalizerBandsCsv,
            bassBoostStrength = prefs[Keys.BASS_BOOST] ?: base.bassBoostStrength,
            virtualizerStrength = prefs[Keys.VIRTUALIZER] ?: base.virtualizerStrength,
            loudnessGainMb = prefs[Keys.LOUDNESS] ?: base.loudnessGainMb,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: base.playbackSpeed,
            playbackPitch = prefs[Keys.PLAYBACK_PITCH] ?: base.playbackPitch
        )
    }

    suspend fun updateSettings(transform: (AppSettingsSnapshot) -> AppSettingsSnapshot) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[Keys.SETTINGS_JSON]
            val current = if (!currentRaw.isNullOrBlank()) {
                runCatching { json.decodeFromString<AppSettingsSnapshot>(currentRaw) }
                    .getOrDefault(AppSettingsSnapshot())
            } else {
                AppSettingsSnapshot()
            }
            val updated = transform(current)
            prefs[Keys.SETTINGS_JSON] = json.encodeToString(updated)
            prefs[Keys.THEME_JSON] = json.encodeToString(updated.themeConfig)
            prefs[Keys.SHUFFLE] = updated.shuffleEnabled
            prefs[Keys.REPEAT] = updated.repeatMode.name
            prefs[Keys.EQ_ENABLED] = updated.equalizerEnabled
            prefs[Keys.EQ_PRESET] = updated.equalizerSelectedPreset
            prefs[Keys.EQ_BANDS] = updated.equalizerBandsCsv
            prefs[Keys.BASS_BOOST] = updated.bassBoostStrength
            prefs[Keys.VIRTUALIZER] = updated.virtualizerStrength
            prefs[Keys.LOUDNESS] = updated.loudnessGainMb
            prefs[Keys.PLAYBACK_SPEED] = updated.playbackSpeed
            prefs[Keys.PLAYBACK_PITCH] = updated.playbackPitch
        }
    }

    suspend fun updateThemeConfig(transform: (ThemeConfig) -> ThemeConfig) {
        updateSettings { it.copy(themeConfig = transform(it.themeConfig)) }
    }

    suspend fun savePlaybackPosition(trackId: Long, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_TRACK_ID] = trackId
            prefs[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    suspend fun updateScreenSort(screenKey: String, pref: ScreenSortPreference) {
        updateSettings {
            val map = it.screenSortPrefs.toMutableMap()
            map[screenKey] = pref
            it.copy(screenSortPrefs = map)
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { it.clear() }
    }

    fun resolveDiscogsToken(snapshot: AppSettingsSnapshot): String {
        return snapshot.discogsTokenOverride.trim()
    }

    fun resolveAcoustIdKey(snapshot: AppSettingsSnapshot): String {
        return snapshot.acoustIdKeyOverride.trim()
    }

    fun resolveMusixmatchKey(snapshot: AppSettingsSnapshot): String {
        return snapshot.musixmatchKeyOverride.trim()
    }
}
