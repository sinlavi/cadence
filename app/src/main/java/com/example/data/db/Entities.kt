package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["contentUri"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["albumArtist"]),
        Index(value = ["genre"]),
        Index(value = ["year"]),
        Index(value = ["folderPath"]),
        Index(value = ["playCount"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["dateAddedSec"]),
        Index(value = ["isFavorite"]),
        Index(value = ["rating"])
    ]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long? = null,
    val contentUri: String,
    val filePath: String = "",
    val fileName: String = "",
    val folderPath: String = "",
    val folderName: String = "",
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val albumArtist: String = "",
    val genre: String = "",
    val composer: String = "",
    val conductor: String = "",
    val comment: String = "",
    val copyright: String = "",
    val publisher: String = "",
    val recordLabel: String = "",
    val encoder: String = "",
    val grouping: String = "",
    val subtitle: String = "",
    val language: String = "",
    val isrc: String = "",
    val musicalKey: String = "",
    val releaseDate: String = "",
    val originalReleaseDate: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val totalTracks: Int = 0,
    val discNumber: Int = 0,
    val totalDiscs: Int = 0,
    val bpm: Int = 0,
    val isCompilation: Boolean = false,
    val durationMs: Long = 0,
    val fileSizeBytes: Long = 0,
    val dateAddedSec: Long = 0,
    val dateModifiedSec: Long = 0,
    val format: String = "",
    val codec: String = "",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
    val channels: Int = 2,
    val replayGainTrackDb: Float? = null,
    val replayGainAlbumDb: Float? = null,
    val albumId: Long = 0,
    val artworkUri: String? = null,
    val hasEmbeddedArtwork: Boolean = false,
    val hasLyrics: Boolean = false,
    val isFavorite: Boolean = false,
    val rating: Int = 0,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val totalListenMs: Long = 0,
    val lastPlayedAt: Long = 0,
    val lastResumePosMs: Long = 0,
    val perTrackSpeed: Float? = null,
    val perTrackVolumeGain: Float = 1.0f,
    val colorLabel: Long? = null,
    val customTags: String = "",
    val normalizedSearch: String = "",
    val isAvailable: Boolean = true,
    val lastScannedAt: Long = 0
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackId"]), Index(value = ["playlistId"])]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "smart_playlists")
data class SmartPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val definitionJson: String,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "lyrics_entries",
    indices = [
        Index(value = ["trackId"], unique = true),
        Index(value = ["fallbackKey"])
    ]
)
data class LyricsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val fallbackKey: String,
    val plainLyrics: String = "",
    val syncedLrc: String = "",
    val isSynced: Boolean = false,
    val isInstrumental: Boolean = false,
    val source: String = "manual",
    val storageMode: String = "APP_DATABASE",
    val externalFileUri: String? = null,
    val userEdited: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "queue_items",
    indices = [Index(value = ["position"]), Index(value = ["trackId"])]
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val queueEntryId: Long = 0,
    val trackId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "queue_history",
    indices = [Index(value = ["playedAt"])]
)
data class QueueHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["trackId"]), Index(value = ["startedAt"]), Index(value = ["dayBucket"])]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val startedAt: Long,
    val listenedMs: Long,
    val completed: Boolean,
    val skipped: Boolean,
    val dayBucket: String // yyyy-MM-dd
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackId"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val label: String,
    val positionMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_themes")
data class CustomThemeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val configJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bandLevelsCsv: String,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val isBuiltIn: Boolean = false
)

@Entity(
    tableName = "tag_edit_history",
    indices = [Index(value = ["trackId"]), Index(value = ["editedAt"])]
)
data class TagEditHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val contentUri: String,
    val beforeTagsJson: String,
    val afterTagsJson: String,
    val editedAt: Long = System.currentTimeMillis(),
    val rolledBack: Boolean = false
)

@Entity(
    tableName = "metadata_cache",
    indices = [Index(value = ["cacheKey"], unique = true), Index(value = ["cachedAt"])]
)
data class MetadataCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cacheKey: String,
    val provider: String,
    val payloadJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scan_folders",
    indices = [Index(value = ["uriOrPath"], unique = true)]
)
data class ScanFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriOrPath: String,
    val displayName: String,
    val isTreeUri: Boolean = false,
    val isExcluded: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["query"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

// Query projection data classes

data class AlbumSummary(
    val album: String,
    val albumArtist: String,
    val albumId: Long,
    val artworkUri: String?,
    val trackCount: Int,
    val totalDurationMs: Long,
    val year: Int
)

data class ArtistSummary(
    val artist: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkUri: String?
)

data class GenreSummary(
    val genre: String,
    val trackCount: Int,
    val totalDurationMs: Long
)

data class FolderSummary(
    val folderPath: String,
    val folderName: String,
    val trackCount: Int,
    val totalDurationMs: Long
)

data class YearSummary(
    val year: Int,
    val trackCount: Int
)

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int,
    val totalDurationMs: Long
)

data class DailyListeningSummary(
    val dayBucket: String,
    val totalMs: Long,
    val playEvents: Int
)

data class LibraryStatsSummary(
    val totalTracks: Int,
    val totalAlbums: Int,
    val totalArtists: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
    val totalListenMs: Long,
    val tracksWithLyrics: Int,
    val tracksWithArtwork: Int
)

data class QueueTrackJoined(
    @ColumnInfo(name = "queueEntryId") val queueEntryId: Long,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "trackId") val trackId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "album") val album: String,
    @ColumnInfo(name = "durationMs") val durationMs: Long,
    @ColumnInfo(name = "contentUri") val contentUri: String,
    @ColumnInfo(name = "artworkUri") val artworkUri: String?,
    @ColumnInfo(name = "codec") val codec: String,
    @ColumnInfo(name = "bitrateKbps") val bitrateKbps: Int,
    @ColumnInfo(name = "sampleRateHz") val sampleRateHz: Int,
    @ColumnInfo(name = "isFavorite") val isFavorite: Boolean
)
