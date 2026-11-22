package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE isAvailable = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @RawQuery(observedEntities = [TrackEntity::class])
    fun observeTracksRaw(query: SupportSQLiteQuery): Flow<List<TrackEntity>>

    @RawQuery
    suspend fun queryTracksRaw(query: SupportSQLiteQuery): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE isAvailable = 1")
    suspend fun getAllAvailableTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun observeTrackById(id: Long): Flow<TrackEntity?>

    @Query("SELECT * FROM tracks WHERE contentUri = :uri LIMIT 1")
    suspend fun getTrackByUri(uri: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<Long>): List<TrackEntity>

    @Query(
        """
        SELECT album,
               COALESCE(NULLIF(albumArtist, ''), artist) AS albumArtist,
               MAX(albumId) AS albumId,
               MAX(artworkUri) AS artworkUri,
               COUNT(*) AS trackCount,
               SUM(durationMs) AS totalDurationMs,
               MAX(year) AS year
        FROM tracks
        WHERE isAvailable = 1
        GROUP BY album, COALESCE(NULLIF(albumArtist, ''), artist)
        ORDER BY album COLLATE NOCASE ASC
        """
    )
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query(
        """
        SELECT artist,
               COUNT(*) AS trackCount,
               COUNT(DISTINCT album) AS albumCount,
               MAX(artworkUri) AS artworkUri
        FROM tracks
        WHERE isAvailable = 1
        GROUP BY artist
        ORDER BY artist COLLATE NOCASE ASC
        """
    )
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT COALESCE(NULLIF(albumArtist, ''), artist) AS artist,
               COUNT(*) AS trackCount,
               COUNT(DISTINCT album) AS albumCount,
               MAX(artworkUri) AS artworkUri
        FROM tracks
        WHERE isAvailable = 1
        GROUP BY COALESCE(NULLIF(albumArtist, ''), artist)
        ORDER BY artist COLLATE NOCASE ASC
        """
    )
    fun observeAlbumArtists(): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT COALESCE(NULLIF(genre, ''), 'Unknown Genre') AS genre,
               COUNT(*) AS trackCount,
               SUM(durationMs) AS totalDurationMs
        FROM tracks
        WHERE isAvailable = 1
        GROUP BY COALESCE(NULLIF(genre, ''), 'Unknown Genre')
        ORDER BY genre COLLATE NOCASE ASC
        """
    )
    fun observeGenres(): Flow<List<GenreSummary>>

    @Query(
        """
        SELECT COALESCE(NULLIF(composer, ''), 'Unknown Composer') AS artist,
               COUNT(*) AS trackCount,
               COUNT(DISTINCT album) AS albumCount,
               MAX(artworkUri) AS artworkUri
        FROM tracks
        WHERE isAvailable = 1 AND composer != ''
        GROUP BY composer
        ORDER BY composer COLLATE NOCASE ASC
        """
    )
    fun observeComposers(): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT year, COUNT(*) AS trackCount
        FROM tracks
        WHERE isAvailable = 1 AND year > 0
        GROUP BY year
        ORDER BY year DESC
        """
    )
    fun observeYears(): Flow<List<YearSummary>>

    @Query(
        """
        SELECT folderPath, folderName, COUNT(*) AS trackCount, SUM(durationMs) AS totalDurationMs
        FROM tracks
        WHERE isAvailable = 1 AND folderPath != ''
        GROUP BY folderPath
        ORDER BY folderName COLLATE NOCASE ASC
        """
    )
    fun observeFolders(): Flow<List<FolderSummary>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE isAvailable = 1 AND (
            normalizedSearch LIKE '%' || :q || '%' OR
            title LIKE '%' || :raw || '%' OR
            artist LIKE '%' || :raw || '%' OR
            album LIKE '%' || :raw || '%' OR
            genre LIKE '%' || :raw || '%' OR
            composer LIKE '%' || :raw || '%' OR
            folderName LIKE '%' || :raw || '%' OR
            customTags LIKE '%' || :raw || '%'
        )
        ORDER BY playCount DESC, title COLLATE NOCASE ASC
        LIMIT 250
        """
    )
    fun searchTracks(q: String, raw: String): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT COUNT(*) AS totalTracks,
               COUNT(DISTINCT album) AS totalAlbums,
               COUNT(DISTINCT artist) AS totalArtists,
               COALESCE(SUM(durationMs), 0) AS totalDurationMs,
               COALESCE(SUM(fileSizeBytes), 0) AS totalSizeBytes,
               COALESCE(SUM(totalListenMs), 0) AS totalListenMs,
               SUM(CASE WHEN hasLyrics = 1 THEN 1 ELSE 0 END) AS tracksWithLyrics,
               SUM(CASE WHEN hasEmbeddedArtwork = 1 OR artworkUri IS NOT NULL THEN 1 ELSE 0 END) AS tracksWithArtwork
        FROM tracks
        WHERE isAvailable = 1
        """
    )
    fun observeLibraryStats(): Flow<LibraryStatsSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>): List<Long>

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :favorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: Long, favorite: Boolean)

    @Query("UPDATE tracks SET rating = :rating WHERE id = :trackId")
    suspend fun setRating(trackId: Long, rating: Int)

    @Query("UPDATE tracks SET hasLyrics = :hasLyrics WHERE id = :trackId")
    suspend fun setHasLyrics(trackId: Long, hasLyrics: Boolean)

    @Query("UPDATE tracks SET customTags = :customTags WHERE id = :trackId")
    suspend fun updateCustomTags(trackId: Long, customTags: String)

    @Query("UPDATE tracks SET customTags = :customTags WHERE id IN (:trackIds)")
    suspend fun updateCustomTagsBatch(trackIds: List<Long>, customTags: String)

    @Query("UPDATE tracks SET lastResumePosMs = :posMs WHERE id = :trackId")
    suspend fun updateResumePosition(trackId: Long, posMs: Long)

    @Query("UPDATE tracks SET perTrackSpeed = :speed WHERE id = :trackId")
    suspend fun updatePerTrackSpeed(trackId: Long, speed: Float?)

    @Query("UPDATE tracks SET perTrackVolumeGain = :gain WHERE id = :trackId")
    suspend fun updatePerTrackVolumeGain(trackId: Long, gain: Float)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :timestamp, totalListenMs = totalListenMs + :listenMs WHERE id = :trackId")
    suspend fun recordPlayCompletion(trackId: Long, timestamp: Long, listenMs: Long)

    @Query("UPDATE tracks SET skipCount = skipCount + 1, totalListenMs = totalListenMs + :listenMs WHERE id = :trackId")
    suspend fun recordSkip(trackId: Long, listenMs: Long)

    @Query("UPDATE tracks SET isAvailable = 0 WHERE contentUri NOT IN (:activeUris)")
    suspend fun markMissingExcept(activeUris: List<String>)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("DELETE FROM tracks WHERE isAvailable = 0")
    suspend fun purgeUnavailableTracks()

    @Query("UPDATE tracks SET playCount = 0, skipCount = 0, totalListenMs = 0, lastPlayedAt = 0")
    suspend fun resetListeningStatistics()
}

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT p.id, p.name, p.description, p.createdAt, p.updatedAt,
               COUNT(pt.trackId) AS trackCount,
               COALESCE(SUM(t.durationMs), 0) AS totalDurationMs
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId
        LEFT JOIN tracks t ON pt.trackId = t.id AND t.isAvailable = 1
        GROUP BY p.id
        ORDER BY p.updatedAt DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllPlaylistsOnce(): List<PlaylistEntity>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId AND t.isAvailable = 1
        ORDER BY pt.position ASC
        """
    )
    fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """
    )
    suspend fun getPlaylistTracksOnce(playlistId: Long): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(crossRefs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Transaction
    suspend fun replacePlaylistTracks(playlistId: Long, trackIds: List<Long>) {
        clearPlaylistTracks(playlistId)
        val now = System.currentTimeMillis()
        val refs = trackIds.mapIndexed { idx, trackId ->
            PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId, position = idx, addedAt = now)
        }
        if (refs.isNotEmpty()) insertPlaylistTracks(refs)
    }

    // Smart Playlists
    @Query("SELECT * FROM smart_playlists ORDER BY isBuiltIn DESC, updatedAt DESC")
    fun observeSmartPlaylists(): Flow<List<SmartPlaylistEntity>>

    @Query("SELECT * FROM smart_playlists")
    suspend fun getAllSmartPlaylistsOnce(): List<SmartPlaylistEntity>

    @Query("SELECT * FROM smart_playlists WHERE id = :id")
    suspend fun getSmartPlaylistById(id: Long): SmartPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSmartPlaylist(entity: SmartPlaylistEntity): Long

    @Query("DELETE FROM smart_playlists WHERE id = :id")
    suspend fun deleteSmartPlaylist(id: Long)
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_entries WHERE trackId = :trackId LIMIT 1")
    fun observeLyricsForTrack(trackId: Long): Flow<LyricsEntity?>

    @Query("SELECT * FROM lyrics_entries WHERE trackId = :trackId LIMIT 1")
    suspend fun getLyricsForTrack(trackId: Long): LyricsEntity?

    @Query("SELECT * FROM lyrics_entries WHERE fallbackKey = :fallbackKey LIMIT 1")
    suspend fun getLyricsByFallbackKey(fallbackKey: String): LyricsEntity?

    @Query("SELECT * FROM lyrics_entries")
    suspend fun getAllLyricsOnce(): List<LyricsEntity>

    @Query("SELECT trackId FROM lyrics_entries WHERE isSynced = 1 OR plainLyrics != ''")
    fun observeTrackIdsWithLyrics(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyrics(entry: LyricsEntity): Long

    @Query("DELETE FROM lyrics_entries WHERE trackId = :trackId")
    suspend fun deleteLyricsForTrack(trackId: Long)
}

@Dao
interface QueueDao {
    @Query(
        """
        SELECT q.queueEntryId, q.position, q.trackId,
               t.title, t.artist, t.album, t.durationMs, t.contentUri, t.artworkUri,
               t.codec, t.bitrateKbps, t.sampleRateHz, t.isFavorite
        FROM queue_items q
        INNER JOIN tracks t ON q.trackId = t.id
        ORDER BY q.position ASC
        """
    )
    fun observeQueue(): Flow<List<QueueTrackJoined>>

    @Query("SELECT trackId FROM queue_items ORDER BY position ASC")
    suspend fun getQueueTrackIdsOnce(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Transaction
    suspend fun replaceQueue(trackIds: List<Long>) {
        clearQueue()
        val now = System.currentTimeMillis()
        insertQueueItems(
            trackIds.mapIndexed { idx, id ->
                QueueItemEntity(trackId = id, position = idx, addedAt = now)
            }
        )
    }

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN queue_history h ON t.id = h.trackId
        ORDER BY h.playedAt DESC
        LIMIT 100
        """
    )
    fun observeQueueHistory(): Flow<List<TrackEntity>>

    @Insert
    suspend fun insertQueueHistory(entry: QueueHistoryEntity)

    @Query("DELETE FROM queue_history")
    suspend fun clearQueueHistory()
}

@Dao
interface HistoryAndStatsDao {
    @Insert
    suspend fun insertHistory(entry: PlaybackHistoryEntity)

    @Query(
        """
        SELECT dayBucket, COALESCE(SUM(listenedMs), 0) AS totalMs, COUNT(*) AS playEvents
        FROM playback_history
        GROUP BY dayBucket
        ORDER BY dayBucket DESC
        LIMIT 30
        """
    )
    fun observeDailyStats(): Flow<List<DailyListeningSummary>>

    @Query("SELECT * FROM playback_history ORDER BY startedAt DESC LIMIT 500")
    suspend fun getRecentHistoryOnce(): List<PlaybackHistoryEntity>

    @Query("DELETE FROM playback_history")
    suspend fun clearPlaybackHistory()

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE trackId = :trackId ORDER BY positionMs ASC")
    fun observeBookmarks(trackId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)
}

@Dao
interface CustomizationAndCacheDao {
    // Custom Themes
    @Query("SELECT * FROM custom_themes ORDER BY createdAt DESC")
    fun observeCustomThemes(): Flow<List<CustomThemeEntity>>

    @Query("SELECT * FROM custom_themes")
    suspend fun getAllCustomThemesOnce(): List<CustomThemeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomTheme(theme: CustomThemeEntity): Long

    @Query("DELETE FROM custom_themes WHERE id = :id")
    suspend fun deleteCustomTheme(id: Long)

    // Equalizer Presets
    @Query("SELECT * FROM equalizer_presets ORDER BY isBuiltIn DESC, name ASC")
    fun observeEqPresets(): Flow<List<EqualizerPresetEntity>>

    @Query("SELECT * FROM equalizer_presets")
    suspend fun getAllEqPresetsOnce(): List<EqualizerPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEqPreset(preset: EqualizerPresetEntity): Long

    @Query("DELETE FROM equalizer_presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomEqPreset(id: Long)

    // Tag Edit History
    @Query("SELECT * FROM tag_edit_history ORDER BY editedAt DESC LIMIT 100")
    fun observeTagHistory(): Flow<List<TagEditHistoryEntity>>

    @Insert
    suspend fun insertTagHistory(entry: TagEditHistoryEntity): Long

    @Query("UPDATE tag_edit_history SET rolledBack = 1 WHERE id = :id")
    suspend fun markTagHistoryRolledBack(id: Long)

    // Metadata Provider Cache
    @Query("SELECT * FROM metadata_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCache(key: String): MetadataCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCache(entry: MetadataCacheEntity)

    @Query("DELETE FROM metadata_cache")
    suspend fun clearMetadataCache()

    // Scan Folders
    @Query("SELECT * FROM scan_folders ORDER BY isExcluded ASC, displayName ASC")
    fun observeScanFolders(): Flow<List<ScanFolderEntity>>

    @Query("SELECT * FROM scan_folders")
    suspend fun getScanFoldersOnce(): List<ScanFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScanFolder(folder: ScanFolderEntity): Long

    @Query("DELETE FROM scan_folders WHERE id = :id")
    suspend fun deleteScanFolder(id: Long)

    // Search History
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 20")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
