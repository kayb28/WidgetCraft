package com.widgetcraft.app.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Definición de los diseños base del catálogo y su esquema de personalización.
 *
 * Un "widget de WidgetCraft" es una configuración (JSONObject) que apunta a un
 * diseño base y añade propiedades y estilo. La misma configuración alimenta el
 * preview del editor y el RemoteViews real en la pantalla de inicio.
 */
object Designs {

    enum class Type { TEXT, MULTILINE, NUMBER, SWITCH, COLOR, DATETIME, APPS, OPTION }

    data class Prop(
        val key: String,
        val labelRes: String,
        val type: Type,
        /** valores por defecto de opciones para type=OPTION, colores válidos para COLOR */
        val options: List<String> = emptyList(),
        val range: IntRange? = null,
        val allowAuto: Boolean = false,
        val maxLength: Int = 120,
    )

    data class Design(
        val id: String,
        val titleRes: String,
        val iconRes: String,
        val props: List<Prop>,
    )

    // ---- claves de estilo global ----
    const val STYLE_BG_COLOR = "bgColor"        // "#RRGGBB" o "auto"
    const val STYLE_BG_ALPHA = "bgAlpha"        // 0..100
    const val STYLE_RADIUS = "radius"           // 0..32 dp
    const val STYLE_TEXT_COLOR = "textColor"    // "#RRGGBB" o "auto"
    const val STYLE_ACCENT = "accent"           // "#RRGGBB"
    const val STYLE_TEXT_SCALE = "textScale"    // 50..150 (%)
    const val STYLE_BOLD = "bold"               // bool

    val styleProps = listOf(
        Prop(STYLE_BG_COLOR, "style_bg_color", Type.COLOR, allowAuto = true),
        Prop(STYLE_BG_ALPHA, "style_bg_alpha", Type.NUMBER, range = 0..100),
        Prop(STYLE_RADIUS, "style_radius", Type.NUMBER, range = 0..32),
        Prop(STYLE_TEXT_COLOR, "style_text_color", Type.COLOR, allowAuto = true),
        Prop(STYLE_ACCENT, "style_accent", Type.COLOR),
        Prop(STYLE_TEXT_SCALE, "style_text_scale", Type.NUMBER, range = 50..150),
        Prop(STYLE_BOLD, "style_bold", Type.SWITCH),
    )

    private fun text(k: String, res: String, max: Int = 60) = Prop(k, res, Type.TEXT, maxLength = max)

    val all = listOf(
        Design("clock", "d_clock", "ic_d_clock", listOf(
            text("label", "def_clock_label"),
            Prop("hora24", "Formato 24 horas", Type.SWITCH),
            Prop("mostrarFecha", "Mostrar la fecha debajo", Type.SWITCH),
        )),
        Design("date", "d_date", "ic_d_date", listOf(
            text("label", "def_date_label"),
            Prop("formato", "Formato de fecha", Type.OPTION,
                options = listOf("Largo — jueves, 18 de noviembre", "Medio — jue 18 nov", "Numérico — 18/11/2026")),
        )),
        Design("battery", "d_battery", "ic_d_battery", listOf(
            text("label", "def_battery_label"),
            Prop("mostrarBarra", "Mostrar barra de nivel", Type.SWITCH),
        )),
        Design("counter", "d_counter", "ic_d_counter", listOf(
            text("label", "def_counter_label"),
            Prop("paso", "Cuánto suma cada toque", Type.NUMBER, range = 1..10),
        )),
        Design("note", "d_note", "ic_d_note", listOf(
            text("label", "def_note_label"),
            text("texto", "Contenido de la nota", max = 400).copy(type = Type.MULTILINE),
        )),
        Design("quote", "d_quote", "ic_d_quote", listOf(
            text("propia", "Tus frases (separa con ;)", max = 600).copy(type = Type.MULTILINE),
        )),
        Design("countdown", "d_countdown", "ic_d_countdown", listOf(
            text("label", "def_countdown_label"),
            Prop("objetivo", "Fecha y hora objetivo", Type.DATETIME),
        )),
        Design("progress", "d_progress", "ic_d_progress", listOf(
            Prop("mostrarDia", "Progreso del día", Type.SWITCH),
            Prop("mostrarMes", "Progreso del mes", Type.SWITCH),
            Prop("mostrarAno", "Progreso del año", Type.SWITCH),
        )),
        Design("timer", "d_timer", "ic_d_timer", listOf(
            text("label", "def_timer_label"),
        )),
        Design("tasks", "d_tasks", "ic_d_tasks", listOf(
            text("t1", "Tarea 1"),
            text("t2", "Tarea 2"),
            text("t3", "Tarea 3"),
            text("t4", "Tarea 4"),
        )),
        Design("device", "d_device", "ic_d_device", listOf(
            text("label", "def_device_label"),
            Prop("mostrarRam", "Memoria RAM en uso", Type.SWITCH),
            Prop("mostrarAlmacenamiento", "Almacenamiento libre", Type.SWITCH),
            Prop("mostrarUptime", "Tiempo encendido", Type.SWITCH),
        )),
        Design("shortcuts", "d_shortcuts", "ic_d_shortcuts", listOf(
            Prop("apps", "Accesos directos", Type.APPS),
        )),
    )

    fun byId(id: String): Design = all.first { it.id == id }

    /** Configuración por defecto de un diseño, lista para editar. */
    fun defaultConfig(designId: String): JSONObject {
        val d = byId(designId)
        val props = JSONObject()
        for (p in d.props) props.put(p.key, defaultOf(p))
        val style = JSONObject()
        style.put(STYLE_BG_COLOR, "auto")
        style.put(STYLE_BG_ALPHA, 88)
        style.put(STYLE_RADIUS, 24)
        style.put(STYLE_TEXT_COLOR, "auto")
        style.put(STYLE_ACCENT, "#7C4DFF")
        style.put(STYLE_TEXT_SCALE, 100)
        style.put(STYLE_BOLD, false)
        return JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("design", designId)
            .put("created", System.currentTimeMillis())
            .put("props", props)
            .put("style", style)
            .put("widgetId", -1)
    }

    private fun defaultOf(p: Prop): Any = when (p.key) {
        "paso" -> 1
        "hora24" -> true
        "mostrarFecha", "mostrarBarra", "mostrarDia", "mostrarMes", "mostrarAno",
        "mostrarRam", "mostrarAlmacenamiento", "mostrarUptime" -> true
        "formato" -> p.options.first()
        "objetivo" -> System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        "apps" -> JSONArray()
        else -> ""
    }

    // Nombres legibles para propiedades sin entrada en strings.xml
    val propLabels = mapOf(
        "hora24" to "Formato 24 horas",
        "mostrarFecha" to "Mostrar la fecha debajo",
        "formato" to "Formato de fecha",
        "mostrarBarra" to "Mostrar barra de nivel",
        "paso" to "Cuánto suma cada toque",
        "texto" to "Contenido de la nota",
        "propia" to "Tus frases (separa con ;)",
        "objetivo" to "Fecha y hora objetivo",
        "mostrarDia" to "Progreso del día",
        "mostrarMes" to "Progreso del mes",
        "mostrarAno" to "Progreso del año",
        "mostrarRam" to "Memoria RAM en uso",
        "mostrarAlmacenamiento" to "Almacenamiento libre",
        "mostrarUptime" to "Tiempo encendido",
        "apps" to "Accesos directos",
        "t1" to "Tarea 1", "t2" to "Tarea 2", "t3" to "Tarea 3", "t4" to "Tarea 4",
        "label" to "Etiqueta",
    )

    fun labelFor(p: Prop): String = propLabels[p.key] ?: p.labelRes
}

/**
 * Persistencia en SharedPreferences: biblioteca de configuraciones + mapa
 * de widget instalado (appWidgetId) → configuración.
 */
object WidgetStore {

    private const val PREFS = "widgetcraft_store"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- biblioteca ----
    fun saveToLibrary(ctx: Context, cfg: JSONObject) {
        prefs(ctx).edit().putString("lib_" + cfg.getString("id"), cfg.toString()).apply()
    }

    fun library(ctx: Context): MutableList<JSONObject> {
        val p = prefs(ctx)
        val out = mutableListOf<JSONObject>()
        for ((k, v) in p.all) {
            if (k.startsWith("lib_")) {
                try { out.add(JSONObject(v.toString())) } catch (_: Exception) { }
            }
        }
        out.sortBy { it.optLong("created") }
        return out
    }

    fun libraryEntry(ctx: Context, configId: String): JSONObject? {
        val s = prefs(ctx).getString("lib_$configId", null) ?: return null
        return try { JSONObject(s) } catch (_: Exception) { null }
    }

    fun deleteLibraryEntry(ctx: Context, configId: String) {
        prefs(ctx).edit().remove("lib_$configId").apply()
        // desengancha widgets instalados que apuntaban a esta config
        for ((k, v) in prefs(ctx).all) {
            if (k.startsWith("wid_") && v == configId) {
                prefs(ctx).edit().remove(k).apply()
            }
        }
    }

    // ---- widgets instalados ----
    fun configForWidget(ctx: Context, widgetId: Int): JSONObject? {
        val configId = prefs(ctx).getString("wid_$widgetId", null) ?: return null
        return libraryEntry(ctx, configId)
    }

    fun assignWidget(ctx: Context, configId: String, widgetId: Int) {
        val cfg = libraryEntry(ctx, configId) ?: return
        cfg.put("widgetId", widgetId)
        saveToLibrary(ctx, cfg)
        prefs(ctx).edit().putString("wid_$widgetId", configId).apply()
    }

    fun widgetsOfConfig(ctx: Context, configId: String): List<Int> {
        val out = mutableListOf<Int>()
        for ((k, v) in prefs(ctx).all) {
            if (k.startsWith("wid_") && v == configId) {
                k.substringAfter("wid_").toIntOrNull()?.let { out.add(it) }
            }
        }
        return out
    }

    fun removeWidget(ctx: Context, widgetId: Int) {
        val configId = prefs(ctx).getString("wid_$widgetId", null) ?: return
        prefs(ctx).edit().remove("wid_$widgetId").apply()
        val cfg = libraryEntry(ctx, configId) ?: return
        val remaining = widgetsOfConfig(ctx, configId)
        cfg.put("widgetId", if (remaining.isEmpty()) -1 else remaining.first())
        saveToLibrary(ctx, cfg)
    }

    /** estado de datos vivos de un widget instalado (contador, cronómetro, cita…). */
    fun stateFor(ctx: Context, widgetId: Int): JSONObject {
        val s = prefs(ctx).getString("st_$widgetId", null)
        return if (s != null) JSONObject(s) else JSONObject()
    }

    fun saveState(ctx: Context, widgetId: Int, state: JSONObject) {
        prefs(ctx).edit().putString("st_$widgetId", state.toString()).apply()
    }

    fun removeState(ctx: Context, widgetId: Int) {
        prefs(ctx).edit().remove("st_$widgetId").apply()
    }

    // ---- ajustes globales ----
    fun themeMode(ctx: Context): String = prefs(ctx).getString("theme", "system") ?: "system"

    fun setThemeMode(ctx: Context, mode: String) {
        prefs(ctx).edit().putString("theme", mode).apply()
    }
}
