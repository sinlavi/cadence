package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.media3.common.Player
import com.example.CadenceApplication
import com.example.MainActivity
import com.example.R
import com.example.core.model.RepeatModeOption
import com.example.ui.components.loadTrackArtworkBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WidgetSnapshot(
    val trackId: Long = -1L,
    val title: String = "Cadence Player",
    val artist: String = "Tap to open library",
    val album: String = "",
    val artworkUri: String? = null,
    val contentUri: String? = null,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF
)

object CadenceWidgetUpdater {
    const val ACTION_TOGGLE_PLAY = "com.example.widget.ACTION_TOGGLE_PLAY"
    const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
    const val ACTION_PREV = "com.example.widget.ACTION_PREV"
    const val ACTION_TOGGLE_FAVORITE = "com.example.widget.ACTION_TOGGLE_FAVORITE"
    const val ACTION_TOGGLE_SHUFFLE = "com.example.widget.ACTION_TOGGLE_SHUFFLE"
    const val ACTION_CYCLE_REPEAT = "com.example.widget.ACTION_CYCLE_REPEAT"
    const val ACTION_SERVICE_WIDGET_SYNC = "com.example.widget.ACTION_SERVICE_WIDGET_SYNC"
    const val EXTRA_COMMAND = "widget_command"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var snapshot: WidgetSnapshot = WidgetSnapshot()
        private set

    @Volatile
    private var cachedArtKey: String? = null

    @Volatile
    private var cachedArtBitmap: Bitmap? = null

    fun updateFromPlayer(
        context: Context,
        player: Player,
        trackId: Long,
        artworkUri: String? = null,
        contentUri: String? = null,
        isFavorite: Boolean = snapshot.isFavorite
    ) {
        val meta = player.currentMediaItem?.mediaMetadata
        val repeat = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
            Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
            else -> RepeatModeOption.OFF
        }
        val title = meta?.title?.toString()?.takeIf { it.isNotBlank() } ?: "Cadence Player"
        val artist = meta?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Offline Library"
        val album = meta?.albumTitle?.toString().orEmpty()
        val resolvedArtUri = artworkUri ?: meta?.artworkUri?.toString()
        val resolvedContentUri = contentUri ?: player.currentMediaItem?.localConfiguration?.uri?.toString()

        snapshot = WidgetSnapshot(
            trackId = trackId,
            title = title,
            artist = artist,
            album = album,
            artworkUri = resolvedArtUri,
            contentUri = resolvedContentUri,
            isPlaying = player.isPlaying,
            isFavorite = isFavorite,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = repeat
        )
        ensureArtworkAndPush(context.applicationContext)
    }

    fun updateFavoriteState(context: Context, trackId: Long, isFavorite: Boolean) {
        if (snapshot.trackId == trackId) {
            snapshot = snapshot.copy(isFavorite = isFavorite)
            pushAllWidgets(context.applicationContext)
        }
    }

    fun handleActionBroadcast(context: Context, intent: Intent, pendingResult: BroadcastReceiverPendingResult?) {
        val action = intent.action ?: return
        when (action) {
            ACTION_TOGGLE_PLAY, ACTION_NEXT, ACTION_PREV -> {
                val keyCode = when (action) {
                    ACTION_TOGGLE_PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    ACTION_NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
                    ACTION_PREV -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    else -> return
                }
                val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    setClassName(context, "androidx.media3.session.MediaButtonReceiver")
                    putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                }
                runCatching { context.sendBroadcast(mediaIntent) }
                pendingResult?.finish()
            }
            ACTION_TOGGLE_FAVORITE -> {
                val appCtx = context.applicationContext as? CadenceApplication
                if (appCtx == null) {
                    pendingResult?.finish()
                    return
                }
                scope.launch {
                    try {
                        val tid = snapshot.trackId
                        if (tid > 0L) {
                            val nextFav = !snapshot.isFavorite
                            appCtx.container.database.trackDao().setFavorite(tid, nextFav)
                            snapshot = snapshot.copy(isFavorite = nextFav)
                            pushAllWidgets(appCtx)
                            appCtx.sendBroadcast(
                                Intent(ACTION_SERVICE_WIDGET_SYNC)
                                    .setPackage(appCtx.packageName)
                                    .putExtra(EXTRA_COMMAND, ACTION_TOGGLE_FAVORITE)
                            )
                        }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_TOGGLE_SHUFFLE -> {
                val appCtx = context.applicationContext as? CadenceApplication
                if (appCtx == null) {
                    pendingResult?.finish()
                    return
                }
                scope.launch {
                    try {
                        val current = appCtx.container.settingsRepository.settingsFlow.first()
                        val next = !current.shuffleEnabled
                        appCtx.container.settingsRepository.updateSettings { it.copy(shuffleEnabled = next) }
                        snapshot = snapshot.copy(shuffleEnabled = next)
                        pushAllWidgets(appCtx)
                        appCtx.sendBroadcast(
                            Intent(ACTION_SERVICE_WIDGET_SYNC)
                                .setPackage(appCtx.packageName)
                                .putExtra(EXTRA_COMMAND, ACTION_TOGGLE_SHUFFLE)
                        )
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_CYCLE_REPEAT -> {
                val appCtx = context.applicationContext as? CadenceApplication
                if (appCtx == null) {
                    pendingResult?.finish()
                    return
                }
                scope.launch {
                    try {
                        val current = appCtx.container.settingsRepository.settingsFlow.first()
                        val next = when (current.repeatMode) {
                            RepeatModeOption.OFF -> RepeatModeOption.ALL
                            RepeatModeOption.ALL -> RepeatModeOption.ONE
                            RepeatModeOption.ONE -> RepeatModeOption.OFF
                        }
                        appCtx.container.settingsRepository.updateSettings { it.copy(repeatMode = next) }
                        snapshot = snapshot.copy(repeatMode = next)
                        pushAllWidgets(appCtx)
                        appCtx.sendBroadcast(
                            Intent(ACTION_SERVICE_WIDGET_SYNC)
                                .setPackage(appCtx.packageName)
                                .putExtra(EXTRA_COMMAND, ACTION_CYCLE_REPEAT)
                        )
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            else -> pendingResult?.finish()
        }
    }

    private fun ensureArtworkAndPush(context: Context) {
        val snap = snapshot
        val artKey = "${snap.trackId}:${snap.artworkUri.orEmpty()}:${snap.contentUri.orEmpty()}"
        if (artKey != cachedArtKey) {
            cachedArtKey = artKey
            if (snap.artworkUri.isNullOrBlank() && snap.contentUri.isNullOrBlank()) {
                cachedArtBitmap = null
            } else {
                scope.launch {
                    val raw = loadTrackArtworkBitmap(context, snap.artworkUri, snap.contentUri, maxDimPx = 192)
                    val rounded = raw?.let { toRoundedWidgetBitmap(it, radiusPx = 18f) }
                    if (cachedArtKey == artKey) {
                        cachedArtBitmap = rounded
                        pushAllWidgets(context)
                    }
                }
            }
        }
        pushAllWidgets(context)
    }

    fun pushAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val snap = snapshot
        val art = cachedArtBitmap

        val compactIds = manager.getAppWidgetIds(ComponentName(context, CadenceWidgetProvider::class.java))
        compactIds.forEach { id -> updateCompactWidget(context, manager, id, snap, art) }

        val miniIds = manager.getAppWidgetIds(ComponentName(context, CadenceMiniWidgetProvider::class.java))
        miniIds.forEach { id -> updateMiniWidget(context, manager, id, snap, art) }

        val largeIds = manager.getAppWidgetIds(ComponentName(context, CadenceLargeWidgetProvider::class.java))
        largeIds.forEach { id -> updateLargeWidget(context, manager, id, snap, art) }

        val artworkIds = manager.getAppWidgetIds(ComponentName(context, CadenceArtworkWidgetProvider::class.java))
        artworkIds.forEach { id -> updateArtworkWidget(context, manager, id, snap, art) }
    }

    fun updateCompactWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        snap: WidgetSnapshot = snapshot,
        art: Bitmap? = cachedArtBitmap
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_cadence_player)
        views.setTextViewText(R.id.widget_title, snap.title)
        views.setTextViewText(R.id.widget_artist, snap.artist)
        bindArtwork(views, art)
        views.setImageViewResource(R.id.widget_play_pause, if (snap.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setImageViewResource(R.id.widget_favorite, if (snap.isFavorite) R.drawable.ic_widget_favorite else R.drawable.ic_widget_favorite_border)

        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_favorite, actionPendingIntent(context, ACTION_TOGGLE_FAVORITE, 10))
        views.setOnClickPendingIntent(R.id.widget_prev, actionPendingIntent(context, ACTION_PREV, 11))
        views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, ACTION_TOGGLE_PLAY, 12))
        views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, ACTION_NEXT, 13))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateMiniWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        snap: WidgetSnapshot = snapshot,
        art: Bitmap? = cachedArtBitmap
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_cadence_mini)
        views.setTextViewText(R.id.widget_title, snap.title)
        bindArtwork(views, art)
        views.setImageViewResource(R.id.widget_play_pause, if (snap.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)

        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, ACTION_TOGGLE_PLAY, 21))
        views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, ACTION_NEXT, 22))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateLargeWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        snap: WidgetSnapshot = snapshot,
        art: Bitmap? = cachedArtBitmap
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_cadence_large)
        views.setTextViewText(R.id.widget_title, snap.title)
        views.setTextViewText(R.id.widget_artist, snap.artist)
        views.setTextViewText(R.id.widget_album, snap.album.ifBlank { "Offline Library" })
        bindArtwork(views, art)
        views.setImageViewResource(R.id.widget_play_pause, if (snap.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setImageViewResource(R.id.widget_favorite, if (snap.isFavorite) R.drawable.ic_widget_favorite else R.drawable.ic_widget_favorite_border)
        views.setImageViewResource(
            R.id.widget_repeat,
            if (snap.repeatMode == RepeatModeOption.ONE) R.drawable.ic_widget_repeat_one else R.drawable.ic_widget_repeat
        )
        views.setInt(
            R.id.widget_shuffle,
            "setAlpha",
            if (snap.shuffleEnabled) 255 else 130
        )
        views.setInt(
            R.id.widget_repeat,
            "setAlpha",
            if (snap.repeatMode != RepeatModeOption.OFF) 255 else 130
        )

        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_favorite, actionPendingIntent(context, ACTION_TOGGLE_FAVORITE, 30))
        views.setOnClickPendingIntent(R.id.widget_shuffle, actionPendingIntent(context, ACTION_TOGGLE_SHUFFLE, 31))
        views.setOnClickPendingIntent(R.id.widget_prev, actionPendingIntent(context, ACTION_PREV, 32))
        views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, ACTION_TOGGLE_PLAY, 33))
        views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, ACTION_NEXT, 34))
        views.setOnClickPendingIntent(R.id.widget_repeat, actionPendingIntent(context, ACTION_CYCLE_REPEAT, 35))
        manager.updateAppWidget(appWidgetId, views)
    }

    fun updateArtworkWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        snap: WidgetSnapshot = snapshot,
        art: Bitmap? = cachedArtBitmap
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_cadence_artwork)
        views.setTextViewText(R.id.widget_title, snap.title)
        views.setTextViewText(R.id.widget_artist, snap.artist)
        bindArtwork(views, art)
        views.setImageViewResource(R.id.widget_play_pause, if (snap.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)

        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_prev, actionPendingIntent(context, ACTION_PREV, 41))
        views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, ACTION_TOGGLE_PLAY, 42))
        views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, ACTION_NEXT, 43))
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun bindArtwork(views: RemoteViews, art: Bitmap?) {
        if (art != null && !art.isRecycled) {
            views.setImageViewBitmap(R.id.widget_artwork, art)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.widget_artwork_placeholder)
        }
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionPendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, CadenceWidgetProvider::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun toRoundedWidgetBitmap(src: Bitmap, radiusPx: Float): Bitmap {
        val target = 160
        val out = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, target, target)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val srcRect = centerCropRect(src.width, src.height)
        canvas.drawBitmap(src, srcRect, rect, paint)
        return out
    }

    private fun centerCropRect(w: Int, h: Int): Rect {
        val size = minOf(w, h)
        val left = (w - size) / 2
        val top = (h - size) / 2
        return Rect(left, top, left + size, top + size)
    }
}

typealias BroadcastReceiverPendingResult = android.content.BroadcastReceiver.PendingResult
