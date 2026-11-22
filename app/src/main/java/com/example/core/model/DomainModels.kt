package com.example.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeBaseMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED_BLACK
}

@Serializable
enum class NowPlayingStyle(val label: String) {
    CLASSIC("Classic"),
    MINIMAL("Minimal"),
    ARTWORK_FOCUSED("Artwork Focused"),
    COMPACT("Compact"),
    FULL_SCREEN("Full Screen")
}

@Serializable
enum class MiniPlayerStyle(val label: String) {
    STANDARD("Docked Bar"),
    ELEVATED_CARD("Floating Card"),
    MINIMAL("Compact Strip")
}

@Serializable
enum class CardAppearance(val label: String) {
    ELEVATED("Elevated Surface"),
    OUTLINED("Crisp Hairline"),
    FLAT("Minimal Flat")
}

@Serializable
enum class UiDensity(val label: String, val rowVerticalPaddingDp: Int) {
    COMPACT("Compact", 6),
    COMFORTABLE("Comfortable", 10),
    SPACIOUS("Spacious", 14)
}

@Serializable
enum class LyricsBackgroundMode(val label: String) {
    THEME("Theme"),
    DARK("Dark"),
    LIGHT("Light"),
    ARTWORK("Artwork")
}

@Serializable
data class ThemeConfig(
    val presetId: String = "material_indigo",
    val baseMode: ThemeBaseMode = ThemeBaseMode.SYSTEM,
    val primaryHex: Long = 0xFF4F7CFF,
    val secondaryHex: Long = 0xFF14B8A6,
    val customBackgroundHex: Long? = null,
    val customSurfaceHex: Long? = null,
    val useArtworkColors: Boolean = false,
    val artworkThemeAppWide: Boolean = false,
    val artworkCornerRadiusDp: Int = 8,
    val cardAppearance: CardAppearance = CardAppearance.ELEVATED,
    val density: UiDensity = UiDensity.COMFORTABLE,
    val fontScale: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val nowPlayingStyle: NowPlayingStyle = NowPlayingStyle.CLASSIC,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.STANDARD
)

@Serializable
enum class NavDestinationKey(val label: String) {
    HOME("Library"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    FILES("Files"),
    PLAYLISTS("Playlists"),
    SONGS("Songs"),
    GENRES("Genres"),
    YEARS("Years"),
    COMPOSERS("Composers"),
    SMART_VIEWS("Smart Views"),
    LIBRARY("Browse"),
    SEARCH("Search"),
    SETTINGS("Settings")
}

@Serializable
data class GeneratedSmartMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val artworkUri: String? = null,
    val fallbackContentUri: String? = null,
    val trackIds: List<Long> = emptyList()
)

@Serializable
enum class HomeSectionKey(val label: String) {
    CONTINUE_LISTENING("Continue Listening"),
    RECENTLY_PLAYED("Recently Played"),
    RECENTLY_ADDED("Recently Added"),
    MOST_PLAYED("Most Played"),
    FAVORITES("Favorite Tracks"),
    SUGGESTED_MIX("Local Discovery Mix"),
    UNFINISHED_ALBUMS("Unfinished Albums"),
    FORGOTTEN_GEMS("Forgotten Gems"),
    RECENT_PLAYLISTS("Playlists")
}

@Serializable
enum class LibraryTabKey(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    ALBUM_ARTISTS("Album Artists"),
    GENRES("Genres"),
    COMPOSERS("Composers"),
    YEARS("Years"),
    FOLDERS("Files"),
    COLLECTIONS("Smart Views")
}

@Serializable
enum class SmartViewCategory(val label: String, val description: String) {
    RECENTLY_ADDED("Recently Added", "Tracks added in the last 30 days"),
    RECENTLY_PLAYED("Recently Played", "Latest listening sessions"),
    MOST_PLAYED("Most Played", "Highest play count in your library"),
    LEAST_PLAYED("Least Played", "Tracks rarely or never played"),
    FREQUENTLY_SKIPPED("Frequently Skipped", "Tracks skipped more than played"),
    FAVORITES("Favorites", "Hearted tracks"),
    UNRATED("Unrated Tracks", "Tracks with 0-star rating"),
    MISSING_ARTWORK("Missing Artwork", "Tracks without embedded or album cover"),
    MISSING_LYRICS("Missing Lyrics", "Tracks without synced or plain lyrics"),
    MISSING_METADATA("Incomplete Metadata", "Unknown artist, album, genre, or year"),
    DUPLICATES("Duplicate Candidates", "Matching title, artist, and duration"),
    LONGEST("Longest Tracks", "Extended mixes, live sets, and audiobooks"),
    SHORTEST("Shortest Tracks", "Interludes, intros, and short tracks")
}

@Serializable
enum class SortField(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    ALBUM_ARTIST("Album Artist"),
    DURATION("Duration"),
    FILE_SIZE("File Size"),
    DATE_ADDED("Date Added"),
    DATE_MODIFIED("Date Modified"),
    TRACK_NUMBER("Track Number"),
    DISC_NUMBER("Disc Number"),
    YEAR("Year"),
    GENRE("Genre"),
    PLAY_COUNT("Play Count"),
    LAST_PLAYED("Last Played"),
    RATING("Rating"),
    BPM("BPM")
}

@Serializable
enum class SortDirection { ASC, DESC }

@Serializable
data class LibraryFilter(
    val minDurationSec: Int? = null,
    val maxDurationSec: Int? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val minRating: Int? = null,
    val folderPath: String? = null,
    val hasLyrics: Boolean? = null,
    val hasArtwork: Boolean? = null,
    val formats: Set<String> = emptySet()
) {
    val isActive: Boolean
        get() = minDurationSec != null || maxDurationSec != null || artist != null ||
            album != null || genre != null || year != null || minRating != null ||
            folderPath != null || hasLyrics != null || hasArtwork != null || formats.isNotEmpty()
}

@Serializable
enum class RepeatModeOption { OFF, ALL, ONE }

@Serializable
enum class RatingSystemMode(val label: String, val description: String) {
    HEARTS("Likes (Hearts)", "Show heart toggle across tracks, widgets, and Now Playing"),
    STARS("5-Star Rating", "Show 1 to 5 star ratings instead of hearts")
}

@Serializable
enum class LyricsStorageMode(val label: String) {
    APP_DATABASE("App Database"),
    EXTERNAL_LRC("External .lrc Sidecar"),
    EMBEDDED_TAG("Embedded Audio Tag")
}

// --- Smart Playlist Rules ---

@Serializable
enum class SmartCombinator(val label: String) { ALL("Match ALL (AND)"), ANY("Match ANY (OR)") }

@Serializable
enum class SmartField(val label: String, val kind: SmartFieldKind) {
    TITLE("Title", SmartFieldKind.TEXT),
    ARTIST("Artist", SmartFieldKind.TEXT),
    ALBUM("Album", SmartFieldKind.TEXT),
    ALBUM_ARTIST("Album Artist", SmartFieldKind.TEXT),
    GENRE("Genre", SmartFieldKind.TEXT),
    COMPOSER("Composer", SmartFieldKind.TEXT),
    FOLDER_PATH("Folder Path", SmartFieldKind.TEXT),
    FORMAT("Audio Format", SmartFieldKind.TEXT),
    LYRICS("Lyrics", SmartFieldKind.PRESENCE_BOOL),
    ARTWORK("Artwork", SmartFieldKind.PRESENCE_BOOL),
    FAVORITE("Favorite", SmartFieldKind.BOOL),
    PLAY_COUNT("Play Count", SmartFieldKind.NUMBER),
    SKIP_COUNT("Skip Count", SmartFieldKind.NUMBER),
    COMPLETION_PCT("Completion % (0-100)", SmartFieldKind.NUMBER),
    RATING("Rating (0-5)", SmartFieldKind.NUMBER),
    DURATION_SEC("Duration (seconds)", SmartFieldKind.NUMBER),
    YEAR("Year", SmartFieldKind.NUMBER),
    BPM("BPM", SmartFieldKind.NUMBER),
    BITRATE("Bitrate (kbps)", SmartFieldKind.NUMBER),
    USER_TAGS("User-Defined Tags", SmartFieldKind.TEXT),
    DATE_ADDED_DAYS_AGO("Days Since Added", SmartFieldKind.NUMBER),
    LAST_PLAYED_DAYS_AGO("Days Since Last Played", SmartFieldKind.NUMBER)
}

@Serializable
enum class SmartFieldKind { TEXT, NUMBER, BOOL, PRESENCE_BOOL }

@Serializable
enum class SmartOperator(val label: String) {
    CONTAINS("contains"),
    NOT_CONTAINS("does not contain"),
    EQUALS("is"),
    NOT_EQUALS("is not"),
    STARTS_WITH("starts with"),
    GREATER_THAN("is greater than"),
    LESS_THAN("is less than"),
    BETWEEN("is between"),
    IS_EMPTY("is empty / missing"),
    IS_NOT_EMPTY("is present"),
    IS_TRUE("is yes"),
    IS_FALSE("is no")
}

@Serializable
data class SmartCondition(
    val id: String,
    val field: SmartField,
    val operator: SmartOperator,
    val value: String = "",
    val secondValue: String = "",
    val exclude: Boolean = false
)

@Serializable
data class SmartRuleGroup(
    val id: String,
    val combinator: SmartCombinator = SmartCombinator.ALL,
    val conditions: List<SmartCondition> = emptyList(),
    val childGroups: List<SmartRuleGroup> = emptyList()
)

@Serializable
data class SmartSortSpec(
    val field: SortField = SortField.PLAY_COUNT,
    val direction: SortDirection = SortDirection.DESC
)

@Serializable
data class SmartPlaylistDefinition(
    val rootGroup: SmartRuleGroup = SmartRuleGroup(id = "root"),
    val sorts: List<SmartSortSpec> = listOf(SmartSortSpec(SortField.DATE_ADDED, SortDirection.DESC)),
    val maxResults: Int = 200
)

// --- LRC Lyrics Models ---

@Serializable
data class LrcWord(
    val timeMs: Long,
    val text: String
)

@Serializable
data class LrcLine(
    val id: String,
    val timeMs: Long?, // null for unsynced lines
    val text: String,
    val words: List<LrcWord> = emptyList()
)

@Serializable
data class ParsedLrc(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val offsetMs: Long = 0L,
    val lines: List<LrcLine> = emptyList(),
    val isSynced: Boolean = false
)

// --- Tag Editing & Metadata Providers ---

@Serializable
data class EditableTrackTags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val trackNumber: String = "",
    val totalTracks: String = "",
    val discNumber: String = "",
    val totalDiscs: String = "",
    val year: String = "",
    val releaseDate: String = "",
    val originalReleaseDate: String = "",
    val genre: String = "",
    val composer: String = "",
    val conductor: String = "",
    val comment: String = "",
    val lyrics: String = "",
    val copyright: String = "",
    val publisher: String = "",
    val recordLabel: String = "",
    val encoder: String = "",
    val bpm: String = "",
    val musicalKey: String = "",
    val isrc: String = "",
    val grouping: String = "",
    val subtitle: String = "",
    val language: String = "",
    val customTags: String = "",
    val isCompilation: Boolean = false,
    val rating: Int = 0
)

@Serializable
enum class ProviderId(val displayName: String, val requiresKey: Boolean) {
    ITUNES("iTunes US Search", false),
    MUSICBRAINZ("MusicBrainz + Cover Art Archive", false),
    DEEZER("Deezer Public API", false),
    APPLE_MUSIC("Legacy (Unused)", true),
    DISCOGS("Discogs Database", true),
    ACOUSTID("AcoustID", true),
    LRCLIB("LRCLIB Lyrics", false),
    MUSIXMATCH("Musixmatch Lyrics", true)
}

@Serializable
data class MetadataCandidate(
    val provider: ProviderId,
    val externalId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val totalTracks: Int? = null,
    val discNumber: Int? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val recordLabel: String? = null,
    val artworkUrl: String? = null,
    val confidenceScore: Float = 0f
)

@Serializable
data class LyricsCandidate(
    val provider: ProviderId,
    val externalId: String,
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val durationSec: Int? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val confidenceScore: Float = 0f
)
