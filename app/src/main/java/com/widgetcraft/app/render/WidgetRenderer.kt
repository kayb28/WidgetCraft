package com.widgetcraft.app.render

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.widgetcraft.app.R
import com.widgetcraft.app.core.Designs
import com.widgetcraft.app.core.StyleEngine
import com.widgetcraft.app.core.WidgetStore
import com.widgetcraft.app.widget.WidgetProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Configuración → RemoteViews. La misma función alimenta el preview del editor
 * (widgetId = -1) y los widgets reales de la pantalla de inicio.
 */
object WidgetRenderer {

    private val QUOTES = listOf(
        "Lo perfecto es enemigo de lo hecho.",
        "Cada día es una página en blanco.",
        "Lo que mides mejora; lo que ignoras, no.",
        "Pequeños pasos, largos caminos.",
        "Empezar es la mitad del trabajo.",
        "La disciplina abre puertas que la motivación no sostiene.",
        "Hoy es buen día para complicarte menos.",
        "Hazlo simple, hazlo dos veces simple.",
        "Aprende algo nuevo y el mundo crece.",
        "Lo difícil de hoy será lo fácil de mañana.",
        "Descansar también es avanzar.",
        "Hecho vale más que perfecto.",
        "El agua no talla la piedra por su fuerza, sino por su constancia.",
        "Menos pestañas abiertas, más foco.",
        "Una tarea a la vez, hasta el final.",
        "Tu yo del futuro agradece lo que hagas hoy.",
        "La mejor hora para empezar fue ayer; la segunda mejor es ahora.",
        "Construye cosas pequeñas que duren.",
        "Respira. Prioriza. Continúa.",
        "La costumbre es segunda naturaleza.",
        "No cuentes los días; haz que los días cuenten.",
        "Quien quiere, encuentra la manera.",
        "El progreso callado también suma.",
        "Deja ir lo que no puedes cambiar.",
    )

    /** Vista principal del diseño según configuración. */
    fun render(ctx: Context, cfg: JSONObject?, widthPx: Int, heightPx: Int, widgetId: Int): RemoteViews {
        if (cfg == null) return renderPlaceholder(ctx, widgetId)
        val design = cfg.optString("design", "clock")
        val props = cfg.optJSONObject("props") ?: JSONObject()
        val style = cfg.optJSONObject("style") ?: JSONObject()
        val night = StyleEngine.isNight(ctx)
        val bg = StyleEngine.background(style, night)
        val text = StyleEngine.textColor(style, night)
        val accent = StyleEngine.accent(style)
        val scale = StyleEngine.textScale(style)
        val bold = StyleEngine.isBold(style)
        val density = ctx.resources.displayMetrics.density
        val radius = StyleEngine.radiusPx(style, density)
        val views = when (design) {
            "clock" -> renderClock(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "date" -> renderDate(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "battery" -> renderBattery(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "counter" -> renderCounter(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "note" -> renderNote(ctx, props, widthPx, heightPx, bg, radius, text, scale, bold, widgetId)
            "quote" -> renderQuote(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "countdown" -> renderCountdown(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "progress" -> renderProgress(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "timer" -> renderTimer(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "tasks" -> renderTasks(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "device" -> renderDevice(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            "shortcuts" -> renderShortcuts(ctx, props, widthPx, heightPx, bg, radius, text, accent, scale, bold, widgetId)
            else -> renderPlaceholder(ctx, widgetId)
        }
        // botón de edición en la esquina
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.wg_edit, WidgetProvider.editorPendingIntent(ctx, widgetId))
        } else {
            views.setViewVisibility(R.id.wg_edit, View.INVISIBLE)
        }
        return views
    }

    private fun base(layout: Int, widthPx: Int, heightPx: Int, bg: Int, radius: Int): RemoteViews {
        val views = RemoteViews("com.widgetcraft.app", layout)
        val bm = StyleEngine.backgroundBitmap(widthPx, heightPx, bg, radius)
        views.setImageViewBitmap(R.id.wg_bg, bm)
        return views
    }

    private fun styleText(views: RemoteViews, id: Int, text: Int, scale: Float, bold: Boolean, sp: Float) {
        views.setTextColor(id, text)
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_DIP, sp * scale)
        val flags = Paint.ANTI_ALIAS_FLAG or (if (bold) 32 else 0) // 32 = FAKE_BOLD_TEXT_FLAG
        views.setInt(id, "setPaintFlags", flags)
    }

    private fun hide(views: RemoteViews, vararg ids: Int) {
        for (id in ids) views.setViewVisibility(id, View.GONE)
    }

    // ---- 12 diseños ----

    private fun renderClock(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_clock, w, h, bg, radius)
        val hora24 = props.optBoolean("hora24", true)
        val fmt = if (hora24) "HH:mm" else "h:mm a"
        views.setCharSequence(R.id.clock_time, "setFormat24Hour", fmt)
        views.setCharSequence(R.id.clock_time, "setFormat12Hour", fmt)
        styleText(views, R.id.clock_time, text, scale, bold, 34f)
        if (props.optBoolean("mostrarFecha", true)) {
            views.setCharSequence(R.id.clock_date, "setFormat24Hour", "EEE d MMM")
            views.setCharSequence(R.id.clock_date, "setFormat12Hour", "EEE d MMM")
            styleText(views, R.id.clock_date, text, scale, false, 12f)
            views.setTextColor(R.id.clock_date, ColorUtils.setAlphaComponent(text, 190))
        } else hide(views, R.id.clock_date)
        views.setTextViewText(R.id.clock_label, props.optString("label", ctx.getString(R.string.def_clock_label)))
        styleText(views, R.id.clock_label, ColorUtils.setAlphaComponent(text, 150), scale, false, 10f)
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.clock_root, WidgetProvider.editorPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderDate(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_date, w, h, bg, radius)
        val formato = props.optString("formato", "")
        val fmt = when {
            formato.startsWith("Medio") -> "EEE d MMM"
            formato.startsWith("Numérico") -> "dd/MM/yyyy"
            else -> "EEEE, d 'de' MMMM"
        }
        views.setCharSequence(R.id.date_full, "setFormat24Hour", fmt)
        views.setCharSequence(R.id.date_full, "setFormat12Hour", fmt)
        styleText(views, R.id.date_full, text, scale, bold, 18f)
        views.setTextViewText(R.id.date_label, props.optString("label", ctx.getString(R.string.def_date_label)))
        styleText(views, R.id.date_label, accent, scale, true, 11f)
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.date_root, WidgetProvider.editorPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderBattery(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_battery, w, h, bg, radius)
        val level = StyleEngine.batteryLevel(ctx)
        val pct = if (level >= 0) level else 0
        views.setTextViewText(R.id.battery_pct, "$pct%")
        styleText(views, R.id.battery_pct, text, scale, true, 30f)
        views.setTextViewText(R.id.battery_label, props.optString("label", ctx.getString(R.string.def_battery_label)))
        styleText(views, R.id.battery_label, ColorUtils.setAlphaComponent(text, 150), scale, false, 10f)
        val charging = StyleEngine.batteryCharging(ctx)
        views.setTextViewText(
            R.id.battery_status,
            if (charging) "⚡ " + ctx.getString(R.string.battery_charging) else "",
        )
        styleText(views, R.id.battery_status, accent, scale, false, 10f)
        if (props.optBoolean("mostrarBarra", true)) {
            views.setProgressBar(R.id.battery_bar, 100, pct, false)
            views.setColorStateList(R.id.battery_bar, "setProgressTintList",
                android.content.res.ColorStateList.valueOf(if (pct <= 15) 0xFFF44336.toInt() else accent))
        } else hide(views, R.id.battery_bar)
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.battery_root, WidgetProvider.refreshPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderCounter(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_counter, w, h, bg, radius)
        val paso = props.optInt("paso", 1).coerceAtLeast(1)
        val count = if (widgetId >= 0) WidgetStore.stateFor(ctx, widgetId).optInt("count", 0) else 0
        views.setTextViewText(R.id.counter_value, "${count * paso}")
        styleText(views, R.id.counter_value, accent, scale, true, 38f)
        views.setTextViewText(R.id.counter_label, props.optString("label", ctx.getString(R.string.def_counter_label)))
        styleText(views, R.id.counter_label, ColorUtils.setAlphaComponent(text, 170), scale, false, 11f)
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.counter_root, WidgetProvider.counterIncPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderNote(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_note, w, h, bg, radius)
        views.setTextViewText(R.id.note_label, props.optString("label", ctx.getString(R.string.def_note_label)))
        styleText(views, R.id.note_label, accent, scale, true, 10f)
        views.setTextViewText(R.id.note_text, props.optString("texto", "Toca el engranaje para editar…"))
        styleText(views, R.id.note_text, text, scale, bold, 14f)
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.note_root, WidgetProvider.editorPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderQuote(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_quote, w, h, bg, radius)
        val pool = ArrayList(QUOTES)
        val propias = props.optString("propia", "").split(';', '；').map { it.trim() }.filter { it.isNotEmpty() }
        pool.addAll(propias)
        if (pool.isEmpty()) pool.add("…")
        val idx = if (widgetId >= 0) WidgetStore.stateFor(ctx, widgetId).optInt("quoteIdx", 0).mod(pool.size) else 0
        views.setTextViewText(R.id.quote_text, "“" + pool[idx] + "”")
        styleText(views, R.id.quote_text, text, scale, bold, 14f)
        views.setTextViewText(R.id.quote_hint, ctx.getString(R.string.def_quote_hint))
        styleText(views, R.id.quote_hint, accent, scale, false, 9f)
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.quote_root, WidgetProvider.nextQuotePendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderCountdown(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_countdown, w, h, bg, radius)
        val target = props.optLong("objetivo", System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)
        val result = StyleEngine.countdown(target, System.currentTimeMillis())
        val label = when (result) {
            is StyleEngine.Period.Done -> ctx.getString(R.string.countdown_done)
            is StyleEngine.Period.Running -> result.label
        }
        views.setTextViewText(R.id.countdown_value, label)
        styleText(views, R.id.countdown_value, accent, scale, true, 28f)
        views.setTextViewText(R.id.countdown_label, props.optString("label", ctx.getString(R.string.def_countdown_label)))
        styleText(views, R.id.countdown_label, ColorUtils.setAlphaComponent(text, 170), scale, false, 11f)
        views.setTextViewText(R.id.countdown_target, StyleEngine.formatDateTime(target))
        styleText(views, R.id.countdown_target, text, scale, false, 10f)
        views.setTextColor(R.id.countdown_target, ColorUtils.setAlphaComponent(text, 140))
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.countdown_root, WidgetProvider.refreshPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderProgress(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_progress, w, h, bg, radius)
        val cal = Calendar.getInstance()
        var visible = 0
        val rows = listOf(
            Triple("mostrarDia" to R.id.row_dia, "dia" to R.id.pct_dia, "Día"),
            Triple("mostrarMes" to R.id.row_mes, "mes" to R.id.pct_mes, "Mes"),
            Triple("mostrarAno" to R.id.row_ano, "ano" to R.id.pct_ano, "Año"),
        )
        for ((showKey, kindKey, title) in rows) {
            val (show, rowId) = showKey
            val (kind, pctId) = kindKey
            if (!props.optBoolean(show, true)) {
                views.setViewVisibility(rowId, View.GONE)
                continue
            }
            visible++
            views.setViewVisibility(rowId, View.VISIBLE)
            val progress = StyleEngine.periodProgress(kind, cal)
            val pct = when (progress) {
                is StyleEngine.Period.Running -> progress.percent
                is StyleEngine.Period.Done -> 100
            }
            views.setProgressBar(barIdOf(kind), 100, pct, false)
            views.setColorStateList(barIdOf(kind), "setProgressTintList",
                android.content.res.ColorStateList.valueOf(accent))
            views.setTextViewText(labelIdOf(kind), title)
            styleText(views, labelIdOf(kind), text, scale, bold, 12f)
            views.setTextViewText(pctId, "$pct%")
            styleText(views, pctId, text, scale, false, 12f)
        }
        if (visible == 0) hide(views, R.id.progress_root)
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.progress_root, WidgetProvider.refreshPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun barIdOf(kind: String) = when (kind) {
        "dia" -> R.id.bar_dia; "mes" -> R.id.bar_mes; else -> R.id.bar_ano
    }

    private fun labelIdOf(kind: String) = when (kind) {
        "dia" -> R.id.label_dia; "mes" -> R.id.label_mes; else -> R.id.label_ano
    }

    private fun renderTimer(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_timer, w, h, bg, radius)
        val st = if (widgetId >= 0) WidgetStore.stateFor(ctx, widgetId) else JSONObject()
        val accum = st.optLong("accum", 0L)
        val running = st.optBoolean("running", false)
        val startedAt = st.optLong("startedAt", 0L)
        val elapsed = accum + (if (running) System.currentTimeMillis() - startedAt else 0)
        views.setTextViewText(R.id.timer_value, if (elapsed > 0) StyleEngine.formatElapsed(elapsed) else ctx.getString(R.string.timer_zero))
        styleText(views, R.id.timer_value, accent, scale, true, 26f)
        views.setTextViewText(R.id.timer_label, props.optString("label", ctx.getString(R.string.def_timer_label)))
        styleText(views, R.id.timer_label, ColorUtils.setAlphaComponent(text, 170), scale, false, 11f)
        views.setTextViewText(R.id.timer_state,
            if (running) "▶ " + ctx.getString(R.string.timer_running) else "■ " + ctx.getString(R.string.timer_stopped))
        styleText(views, R.id.timer_state, text, scale, false, 10f)
        views.setTextColor(R.id.timer_state, ColorUtils.setAlphaComponent(text, 150))
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.timer_root, WidgetProvider.timerTogglePendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderTasks(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_tasks, w, h, bg, radius)
        val done = if (widgetId >= 0) {
            val a = WidgetStore.stateFor(ctx, widgetId).optJSONArray("done") ?: JSONArray()
            (0 until a.length()).map { a.optBoolean(it) }.toMutableList()
        } else mutableListOf(false, false, false, false)
        while (done.size < 4) done.add(false)
        val ids = listOf(R.id.task_1, R.id.task_2, R.id.task_3, R.id.task_4)
        for (i in 0 until 4) {
            val taskText = props.optString("t${i + 1}", "").trim()
            val id = ids[i]
            if (taskText.isEmpty()) {
                views.setViewVisibility(id, View.GONE)
                continue
            }
            views.setViewVisibility(id, View.VISIBLE)
            val mark = if (done[i]) "☑ " else "☐ "
            val span = SpannableString(mark + taskText)
            if (done[i]) span.setSpan(StrikethroughSpan(), mark.length, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            views.setTextViewText(id, span)
            styleText(views, id, if (done[i]) ColorUtils.setAlphaComponent(text, 130) else text, scale, bold, 13f)
            if (widgetId >= 0) {
                views.setOnClickPendingIntent(id, WidgetProvider.taskTogglePendingIntent(ctx, widgetId, i))
            }
        }
        return views
    }

    private fun renderDevice(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_device, w, h, bg, radius)
        views.setTextViewText(R.id.device_label, props.optString("label", ctx.getString(R.string.def_device_label)))
        styleText(views, R.id.device_label, accent, scale, true, 10f)
        val ids = mapOf(
            "ram" to (R.id.dev_ram to props.optBoolean("mostrarRam", true)),
            "storage" to (R.id.dev_storage to props.optBoolean("mostrarAlmacenamiento", true)),
            "uptime" to (R.id.dev_uptime to props.optBoolean("mostrarUptime", true)),
        )
        val ram = StyleEngine.ramUsedPercent(ctx)
        val storage = StyleEngine.storageFreeGb()
        val uptime = StyleEngine.uptimeDays()
        for ((_, pair) in ids) {
            val (id, show) = pair
            if (!show) { views.setViewVisibility(id, View.GONE); continue }
            val value = when (id) {
                R.id.dev_ram -> "RAM: $ram% en uso"
                R.id.dev_storage -> "Libre: $storage GB"
                else -> if (uptime > 0) "Encendido: $uptime d" else "Encendido: menos de 1 d"
            }
            views.setTextViewText(id, value)
            styleText(views, id, text, scale, bold, 13f)
        }
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.device_root, WidgetProvider.refreshPendingIntent(ctx, widgetId))
        }
        return views
    }

    private fun renderShortcuts(
        ctx: Context, props: JSONObject, w: Int, h: Int, bg: Int, radius: Int,
        text: Int, accent: Int, scale: Float, bold: Boolean, widgetId: Int,
    ): RemoteViews {
        val views = base(R.layout.wg_shortcuts, w, h, bg, radius)
        val apps = props.optJSONArray("apps") ?: JSONArray()
        val ids = listOf(R.id.shortcut_1, R.id.shortcut_2, R.id.shortcut_3, R.id.shortcut_4)
        for (i in ids.indices) {
            val id = ids[i]
            if (i >= apps.length()) { views.setViewVisibility(id, View.GONE); continue }
            val app = apps.optJSONObject(i) ?: continue
            val label = app.optString("label", app.optString("pkg", ""))
            views.setViewVisibility(id, View.VISIBLE)
            views.setTextViewText(id, "▸ $label")
            styleText(views, id, text, scale, bold, 14f)
            if (widgetId >= 0) {
                views.setOnClickPendingIntent(id, WidgetProvider.launchAppPendingIntent(ctx, widgetId, i, app.optString("pkg", "")))
            }
        }
        return views
    }

    /** Widget instalado sin configurar: invita a tocar para configurarlo. */
    private fun renderPlaceholder(ctx: Context, widgetId: Int): RemoteViews {
        val views = base(R.layout.wg_placeholder, 110 * 3, 70 * 3, 0x886750A4.toInt(), 24)
        views.setTextViewText(R.id.placeholder_text, ctx.getString(R.string.placeholder_text))
        views.setTextColor(R.id.placeholder_text, 0xFFFFFFFF.toInt())
        if (widgetId >= 0) {
            views.setOnClickPendingIntent(R.id.placeholder_root, WidgetProvider.editorPendingIntent(ctx, widgetId))
        }
        return views
    }

    // ---- actualización real ----

    fun pushWidget(ctx: Context, widgetId: Int) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val cfg = WidgetStore.configForWidget(ctx, widgetId)
        val opts: Bundle = mgr.getAppWidgetOptions(widgetId)
        val density = ctx.resources.displayMetrics.density
        val w = (opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110).coerceAtLeast(40) * density).toInt()
        val h = (opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 70).coerceAtLeast(30) * density).toInt()
        mgr.updateAppWidget(widgetId, render(ctx, cfg, w, h, widgetId))
    }

    fun pushAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        for (id in mgr.getAppWidgetIds(WidgetProvider.component(ctx))) {
            pushWidget(ctx, id)
        }
    }
}
