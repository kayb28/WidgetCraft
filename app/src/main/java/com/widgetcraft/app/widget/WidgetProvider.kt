package com.widgetcraft.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.widgetcraft.app.core.WidgetStore
import com.widgetcraft.app.render.WidgetRenderer
import com.widgetcraft.app.ui.EditorActivity
import org.json.JSONObject

/**
 * Único proveedor de widgets de la app: cualquier widget instalado apunta a una
 * configuración guardada en WidgetStore y se pinta con WidgetRenderer.
 */
class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetRenderer.pushWidget(context, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle?) {
        // redimensionado: repinta con el nuevo tamaño
        WidgetRenderer.pushWidget(context, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetStore.removeWidget(context, id)
            WidgetStore.removeState(context, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (widgetId < 0) return
        val state = WidgetStore.stateFor(context, widgetId)
        when (intent.action) {
            ACTION_INC_COUNTER -> {
                state.put("count", state.optInt("count", 0) + 1)
                WidgetStore.saveState(context, widgetId, state)
                WidgetRenderer.pushWidget(context, widgetId)
            }
            ACTION_NEXT_QUOTE -> {
                state.put("quoteIdx", state.optInt("quoteIdx", 0) + 1)
                WidgetStore.saveState(context, widgetId, state)
                WidgetRenderer.pushWidget(context, widgetId)
            }
            ACTION_TIMER_TOGGLE -> {
                val running = state.optBoolean("running", false)
                if (running) {
                    val startedAt = state.optLong("startedAt", 0L)
                    state.put("accum", state.optLong("accum", 0L) + (System.currentTimeMillis() - startedAt))
                    state.put("running", false)
                } else {
                    state.put("running", true)
                    state.put("startedAt", System.currentTimeMillis())
                }
                WidgetStore.saveState(context, widgetId, state)
                WidgetRenderer.pushWidget(context, widgetId)
            }
            ACTION_TOGGLE_TASK -> {
                val row = intent.getIntExtra(EXTRA_ROW, 0)
                val done = state.optJSONArray("done") ?: org.json.JSONArray()
                while (done.length() <= row) done.put(false)
                done.put(row, !done.optBoolean(row))
                state.put("done", done)
                WidgetStore.saveState(context, widgetId, state)
                WidgetRenderer.pushWidget(context, widgetId)
            }
            ACTION_REFRESH -> WidgetRenderer.pushWidget(context, widgetId)
        }
    }

    companion object {
        const val ACTION_INC_COUNTER = "com.widgetcraft.app.INC_COUNTER"
        const val ACTION_NEXT_QUOTE = "com.widgetcraft.app.NEXT_QUOTE"
        const val ACTION_TIMER_TOGGLE = "com.widgetcraft.app.TIMER_TOGGLE"
        const val ACTION_TOGGLE_TASK = "com.widgetcraft.app.TOGGLE_TASK"
        const val ACTION_REFRESH = "com.widgetcraft.app.REFRESH"
        const val EXTRA_ROW = "row"
        private const val REQUEST_BASE = 1000

        fun component(ctx: Context): ComponentName = ComponentName(ctx, WidgetProvider::class.java)

        private fun broadcast(ctx: Context, widgetId: Int, action: String, requestCode: Int, extras: Intent.() -> Unit = {}): PendingIntent {
            val intent = Intent(ctx, WidgetProvider::class.java)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .setData(Uri.parse("widgetcraft://$action/$widgetId/$requestCode"))
            intent.extras()
            return PendingIntent.getBroadcast(
                ctx, REQUEST_BASE + requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun editorPendingIntent(ctx: Context, widgetId: Int): PendingIntent {
            val intent = Intent(ctx, EditorActivity::class.java)
                .setAction(EditorActivity.ACTION_EDIT_WIDGET)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                ctx, REQUEST_BASE + widgetId * 8 + 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun refreshPendingIntent(ctx: Context, widgetId: Int): PendingIntent =
            broadcast(ctx, widgetId, ACTION_REFRESH, widgetId * 8 + 2)

        fun counterIncPendingIntent(ctx: Context, widgetId: Int): PendingIntent =
            broadcast(ctx, widgetId, ACTION_INC_COUNTER, widgetId * 8 + 3)

        fun nextQuotePendingIntent(ctx: Context, widgetId: Int): PendingIntent =
            broadcast(ctx, widgetId, ACTION_NEXT_QUOTE, widgetId * 8 + 4)

        fun timerTogglePendingIntent(ctx: Context, widgetId: Int): PendingIntent =
            broadcast(ctx, widgetId, ACTION_TIMER_TOGGLE, widgetId * 8 + 5)

        fun taskTogglePendingIntent(ctx: Context, widgetId: Int, row: Int): PendingIntent =
            broadcast(ctx, widgetId, ACTION_TOGGLE_TASK, widgetId * 8 + 6) { putExtra(EXTRA_ROW, row) }

        fun launchAppPendingIntent(ctx: Context, widgetId: Int, row: Int, pkg: String): PendingIntent {
            val launch = ctx.packageManager.getLaunchIntentForPackage(pkg)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                ctx, REQUEST_BASE + widgetId * 8 + 7 + row, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
