package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player

class CadenceWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            CadenceWidgetUpdater.updateCompactWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action in HANDLED_ACTIONS) {
            val pending = if (action in ASYNC_ACTIONS) goAsync() else null
            CadenceWidgetUpdater.handleActionBroadcast(context, intent, pending)
        }
    }

    companion object {
        private val ASYNC_ACTIONS = setOf(
            CadenceWidgetUpdater.ACTION_TOGGLE_FAVORITE,
            CadenceWidgetUpdater.ACTION_TOGGLE_SHUFFLE,
            CadenceWidgetUpdater.ACTION_CYCLE_REPEAT
        )
        private val HANDLED_ACTIONS = ASYNC_ACTIONS + setOf(
            CadenceWidgetUpdater.ACTION_TOGGLE_PLAY,
            CadenceWidgetUpdater.ACTION_NEXT,
            CadenceWidgetUpdater.ACTION_PREV
        )

        fun requestWidgetUpdate(context: Context, player: Player, trackId: Long = -1L) {
            CadenceWidgetUpdater.updateFromPlayer(context, player, trackId)
        }
    }
}

class CadenceMiniWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            CadenceWidgetUpdater.updateMiniWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

class CadenceLargeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            CadenceWidgetUpdater.updateLargeWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

class CadenceArtworkWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            CadenceWidgetUpdater.updateArtworkWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
