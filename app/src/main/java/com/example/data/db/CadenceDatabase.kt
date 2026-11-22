package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        SmartPlaylistEntity::class,
        LyricsEntity::class,
        QueueItemEntity::class,
        QueueHistoryEntity::class,
        PlaybackHistoryEntity::class,
        BookmarkEntity::class,
        CustomThemeEntity::class,
        EqualizerPresetEntity::class,
        TagEditHistoryEntity::class,
        MetadataCacheEntity::class,
        ScanFolderEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CadenceDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun queueDao(): QueueDao
    abstract fun historyAndStatsDao(): HistoryAndStatsDao
    abstract fun customizationAndCacheDao(): CustomizationAndCacheDao

    companion object {
        @Volatile
        private var INSTANCE: CadenceDatabase? = null

        fun getInstance(context: Context): CadenceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CadenceDatabase::class.java,
                    "cadence_library.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys=ON;")
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
