package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.backup.BackupManager
import com.example.data.db.CadenceDatabase
import com.example.data.db.MetadataCacheDaoHelper
import com.example.data.lyrics.LyricsRepository
import com.example.data.metadata.MetadataRepository
import com.example.data.preferences.SettingsRepository
import com.example.data.scanner.LibraryScanner
import com.example.data.smart.SmartPlaylistEvaluator
import com.example.data.tagger.TagEditorEngine
import com.example.player.AudioEffectsController
import com.example.player.PlaybackDiagnostics
import com.example.player.PlayerConnection
import com.example.player.SleepTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(private val application: Application) {
    val database: CadenceDatabase = CadenceDatabase.getInstance(application)
    val settingsRepository: SettingsRepository = SettingsRepository(application)
    val cacheHelper: MetadataCacheDaoHelper = MetadataCacheDaoHelper(database.customizationAndCacheDao())

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val audioEffectsController = AudioEffectsController()
    val playbackDiagnostics = PlaybackDiagnostics()
    var sleepTimerController: SleepTimerController? = null

    val tagEditorEngine = TagEditorEngine(
        context = application,
        db = database
    )

    val libraryScanner = LibraryScanner(
        context = application,
        db = database,
        settingsRepository = settingsRepository,
        tagEditorEngine = tagEditorEngine
    )

    val lyricsRepository = LyricsRepository(
        context = application,
        lyricsDao = database.lyricsDao(),
        trackDao = database.trackDao(),
        settingsRepository = settingsRepository,
        cacheHelper = cacheHelper,
        okHttpClient = okHttpClient
    )

    val metadataRepository = MetadataRepository(
        settingsRepository = settingsRepository,
        cacheHelper = cacheHelper,
        okHttpClient = okHttpClient
    )

    val backupManager = BackupManager(
        context = application,
        db = database,
        settingsRepository = settingsRepository
    )

    val playerConnection = PlayerConnection(
        context = application,
        trackDao = database.trackDao(),
        queueDao = database.queueDao(),
        settingsRepository = settingsRepository
    )

    val lyricsSyncEngine = com.example.data.lyrics.LyricsSyncEngine(
        playerConnection = playerConnection,
        lyricsRepository = lyricsRepository,
        trackDao = database.trackDao(),
        tagEditorEngine = tagEditorEngine
    )

    fun seedInitialSmartPlaylistsIfNeeded() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val existing = database.playlistDao().getAllSmartPlaylistsOnce()
            if (existing.isEmpty()) {
                SmartPlaylistEvaluator.defaultBuiltInSmartPlaylists().forEach {
                    database.playlistDao().upsertSmartPlaylist(it)
                }
            }
        }
    }
}

class CadenceApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        container = AppContainer(this)
        container.seedInitialSmartPlaylistsIfNeeded()
        container.playerConnection.connect()
    }

    override fun newImageLoader(): ImageLoader {
        // Bounded memory & disk cache for fast scrolling and 2GB RAM compatibility
        return ImageLoader.Builder(this)
            .components {
                add(com.example.ui.components.EmbeddedAudioArtworkKeyer())
                add(com.example.ui.components.EmbeddedAudioArtworkFetcher.Factory(this@CadenceApplication))
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_artwork_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .crossfade(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                "cadence_playback_channel",
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_playback_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
