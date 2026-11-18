package com.widgetcraft.app.core

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.core.graphics.ColorUtils
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.luminance

/**
 * Resolución de colores (claro/oscuro automático con contraste garantizado),
 * generación del fondo redondeado y cálculos de tiempo compartidos por el
 * preview del editor y los widgets reales.
 */
object StyleEngine {

    const val AUTO = "auto"

    // ---- colores ----
    fun isNight(ctx: Context): Boolean =
        (ctx.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    fun autoBg(night: Boolean): Int = if (night) 0xFF1C1B1F.toInt() else 0xFFFFFBFE.toInt()

    fun autoText(night: Boolean): Int = if (night) 0xFFE6E1E5.toInt() else 0xFF1B1B1F.toInt()

    fun defaultAccent(): Int = 0xFF7C4DFF.toInt()

    /** #RRGGBB → Int. Devuelve null si el formato no es válido. */
    fun parseColor(hex: String?): Int? {
        if (hex.isNullOrBlank() || !hex.startsWith("#")) return null
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (_: Exception) {
            null
        }
    }

    /** Color de fondo final según estilo y modo del sistema. */
    fun background(style: JSONObject, night: Boolean): Int {
        val base = parseColor(style.optString(Designs.STYLE_BG_COLOR, AUTO)) ?: autoBg(night)
        val alpha = style.optInt(Designs.STYLE_BG_ALPHA, 88).coerceIn(0, 100)
        val a = (alpha * 255) / 100
        return (a shl 24) or (base and 0x00FFFFFF)
    }

    /** Texto legible sobre el fondo: automático por luminancia o color elegido. */
    fun textColor(style: JSONObject, night: Boolean): Int {
        val chosen = parseColor(style.optString(Designs.STYLE_TEXT_COLOR, AUTO))
        if (chosen != null) return chosen
        // contraste respecto al fondo efectivo (puede ser translúcido sobre el launcher)
        val bg = background(style, night) and 0xFFFFFF
        val l = ColorUtils.luminance(bg)
        return if (l > 0.45f) autoText(false) else autoText(true)
    }

    fun accent(style: JSONObject): Int =
        parseColor(style.optString(Designs.STYLE_ACCENT, "")) ?: defaultAccent()

    fun textScale(style: JSONObject): Float = style.optInt(Designs.STYLE_TEXT_SCALE, 100).coerceIn(50, 150) / 100f

    fun isBold(style: JSONObject): Boolean = style.optBoolean(Designs.STYLE_BOLD, false)

    fun radiusPx(style: JSONObject, density: Float): Int =
        (style.optInt(Designs.STYLE_RADIUS, 24) * density).toInt()

    /** Fondo del widget: rectángulo redondeado renderizado a bitmap (RemoteViews no acepta drawables arbitrarios). */
    fun backgroundBitmap(widthPx: Int, heightPx: Int, color: Int, radiusPx: Int): Bitmap {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val r = radiusPx.coerceAtMost(minOf(w, h) / 2f)
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, paint)
        return bm
    }

    // ---- cálculos de tiempo (puros, testeables) ----
    sealed class Period {
        data class Done(val percent: Int) : Period()
        data class Running(val percent: Int, val label: String) : Period()
    }

    /** Porcentaje de avance del día / mes / año en [0,100]. */
    fun periodProgress(kind: String, cal: Calendar): Period {
        return when (kind) {
            "dia" -> {
                val total = 24 * 60
                val gone = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val pct = (gone * 100 / total).coerceIn(0, 100)
                Period.Running(pct, String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
            }
            "mes" -> {
                val total = cal.getActualMaximum(Calendar.DAY_OF_MONTH) * 24 * 60
                val gone = (cal.get(Calendar.DAY_OF_MONTH) - 1) * 24 * 60 + cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val pct = (gone * 100 / total).coerceIn(0, 100)
                Period.Running(pct, "día ${cal.get(Calendar.DAY_OF_MONTH)}")
            }
            else -> { // año
                val isLeap = cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365
                val total = (if (isLeap) 366 else 365) * 24 * 60
                val dayOfYear = cal.get(Calendar.DAY_OF_YEAR) - 1
                val gone = dayOfYear * 24 * 60 + cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val pct = (gone * 100 / total).coerceIn(0, 100)
                Period.Running(pct, "día $dayOfYear")
            }
        }
    }

    /** Desglose de tiempo restante hasta un instante; pasado → Done. */
    fun countdown(target: Long, now: Long): Period {
        val diff = target - now
        if (diff <= 0) return Period.Done(100)
        val days = diff / (24 * 60 * 60 * 1000)
        val hours = (diff / (60 * 60 * 1000)) % 24
        val mins = (diff / (60 * 1000)) % 60
        val label = if (days > 0) "${days}d ${hours}h" else if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        return Period.Running(((diff.toDouble() / (30L * 24 * 60 * 60 * 1000)) * 100).coerceIn(0.0, 100.0).toInt(), label)
    }

    /** Milisegundos → "1d 03:24:15" / "03:24:15". */
    fun formatElapsed(ms: Long): String {
        val total = ms.coerceAtLeast(0) / 1000
        val d = total / 86400
        val h = (total % 86400) / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        val core = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        return if (d > 0) "${d}d $core" else core
    }

    /** Fecha objetivo → texto "jue 25 dic, 09:00" (español). */
    fun formatDateTime(epochMs: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        val dias = arrayOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb")
        val meses = arrayOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        return String.format(
            Locale.US, "%s %d %s, %02d:%02d",
            dias[cal.get(Calendar.DAY_OF_WEEK) - 1],
            cal.get(Calendar.DAY_OF_MONTH),
            meses[cal.get(Calendar.MONTH)],
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        )
    }

    // ---- estadísticas de dispositivo (necesitan Context; usadas por el diseño 'device') ----
    fun ramUsedPercent(ctx: Context): Int {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        if (mi.totalMem <= 0) return 0
        return (((mi.totalMem - mi.availMem) * 100) / mi.totalMem).toInt().coerceIn(0, 100)
    }

    fun storageFreeGb(): Double {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBytes / (1024.0 * 1024 * 1024 * 10)).toInt() / 10.0
        } catch (_: Exception) {
            0.0
        }
    }

    fun uptimeDays(): Long = android.os.SystemClock.elapsedRealtime() / (24 * 60 * 60 * 1000L)

    fun batteryLevel(ctx: Context): Int {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 1..100) level else -1
    }

    fun batteryCharging(ctx: Context): Boolean {
        val intent = ctx.registerReceiver(null, android.content.IntentFilter(BatteryManager.ACTION_BATTERY_CHANGED)) ?: return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
}
