package com.widgetcraft.app.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.widgetcraft.app.R
import com.widgetcraft.app.core.Designs
import com.widgetcraft.app.core.StyleEngine
import com.widgetcraft.app.core.WidgetStore
import com.widgetcraft.app.render.WidgetRenderer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Editor de widgets: elige diseño base + personaliza estilo y propiedades con
 * preview real (las mismas RemoteViews que verá la pantalla de inicio).
 *
 * Modos: nuevo desde catálogo (EXTRA_DESIGN_ID), editar de la biblioteca
 * (EXTRA_CONFIG_ID), editar widget instalado por toque (ACTION_EDIT_WIDGET) y
 * flujo de configuración al añadir el widget (EXTRA_APPWIDGET_ID del sistema).
 */
class EditorActivity : AppCompatActivity() {

    companion object {
        const val ACTION_EDIT_WIDGET = "com.widgetcraft.app.EDIT_WIDGET"
        const val EXTRA_DESIGN_ID = "designId"
        const val EXTRA_CONFIG_ID = "configId"
    }

    private lateinit var cfg: JSONObject
    private var configureWidgetId = -1
    private lateinit var previewHost: FrameLayout
    private lateinit var styleHost: LinearLayout
    private lateinit var designHost: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        setTitle(R.string.editor_title)

        previewHost = findViewById(R.id.preview_host)
        styleHost = findViewById(R.id.style_host)
        designHost = findViewById(R.id.design_host)
        val designSpinner = findViewById<Spinner>(R.id.design_spinner)

        when {
            intent.action == ACTION_EDIT_WIDGET -> {
                val widgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                configureWidgetId = widgetId
                cfg = WidgetStore.configForWidget(this, widgetId)
                    ?: Designs.defaultConfig(Designs.all.first().id)
            }
            intent.hasExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID) -> {
                configureWidgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                cfg = Designs.defaultConfig(Designs.all.first().id)
            }
            intent.hasExtra(EXTRA_CONFIG_ID) -> {
                cfg = WidgetStore.libraryEntry(this, intent.getStringExtra(EXTRA_CONFIG_ID)!!)
                    ?: Designs.defaultConfig(Designs.all.first().id)
            }
            else -> cfg = Designs.defaultConfig(intent.getStringExtra(EXTRA_DESIGN_ID) ?: Designs.all.first().id)
        }

        val titles = Designs.all.map { designTitle(it) }
        designSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, titles)
        designSpinner.setSelection(Designs.all.indexOfFirst { it.id == cfg.optString("design") }.coerceAtLeast(0))
        designSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (Designs.all[pos].id != cfg.optString("design")) switchDesign(Designs.all[pos].id)
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener { saveAndFinish(-1) }
        findViewById<Button>(R.id.btn_apply).setOnClickListener { applyFlow() }
        findViewById<Button>(R.id.btn_pin).setOnClickListener { pinFlow() }

        rebuildForm()
        updatePreview()
    }

    /** título legible de un diseño (titleRes es un nombre de recurso, no un id) */
    private fun designTitle(design: Designs.Design): String {
        val resId = resources.getIdentifier(design.titleRes, "string", packageName)
        return if (resId != 0) getString(resId) else design.titleRes
    }

    private fun switchDesign(designId: String) {        val style = cfg.optJSONObject("style") ?: JSONObject()
        val newCfg = Designs.defaultConfig(designId)
        newCfg.put("id", cfg.optString("id", newCfg.optString("id")))
        newCfg.put("style", style)
        cfg = newCfg
        rebuildForm()
        updatePreview()
    }

    private fun rebuildForm() {
        val design = Designs.byId(cfg.optString("design", "clock"))
        FormBuilder.build(this, Designs.styleProps, cfg.getJSONObject("style"), styleHost) { updatePreview() }
        FormBuilder.build(this, design.props, cfg.getJSONObject("props"), designHost) { updatePreview() }
    }

    fun updatePreview() {
        val d = resources.displayMetrics.density
        val w = (330 * d).toInt()
        val h = (200 * d).toInt()
        previewHost.removeAllViews()
        try {
            val rv = WidgetRenderer.render(this, cfg, w, h, -1)
            previewHost.addView(rv.apply(this, previewHost))
        } catch (e: Exception) {
            previewHost.addView(TextView(this).apply { text = "error de preview: ${e.message}" })
        }
    }

    private fun saveConfig() {
        WidgetStore.saveToLibrary(this, cfg)
    }

    private fun saveAndFinish(assignTo: Int) {
        saveConfig()
        if (assignTo >= 0) {
            WidgetStore.assignWidget(this, cfg.getString("id"), assignTo)
            WidgetRenderer.pushWidget(this, assignTo)
            setResult(Activity.RESULT_OK, Intent().putExtra(
                android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, assignTo))
        } else {
            // edición de una configuración ya instalada: refresca los widgets que la usan
            WidgetRenderer.pushAll(this)
        }
        finish()
    }

    private fun applyFlow() {
        saveConfig()
        val configId = cfg.getString("id")
        if (configureWidgetId >= 0) {
            saveAndFinish(configureWidgetId)
            return
        }
        val mgr = android.appwidget.AppWidgetManager.getInstance(this)
        val all = mgr.getAppWidgetIds(com.widgetcraft.app.widget.WidgetProvider.component(this)).toList()
        if (all.isEmpty()) {
            AlertDialog.Builder(this).setMessage(R.string.no_installed_widgets)
                .setPositiveButton(android.R.string.ok, null).show()
            return
        }
        val labels = all.map { "#$it" }.toTypedArray()
        AlertDialog.Builder(this).setTitle(R.string.choose_widget)
            .setItems(labels) { _, which -> saveAndFinish(all[which]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pinFlow() {
        saveConfig()
        val mgr = android.appwidget.AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(
                com.widgetcraft.app.widget.WidgetProvider.component(this), null, null)
            AlertDialog.Builder(this).setMessage(R.string.pin_requested)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener { finish() }
                .show()
        }
    }
}

/** Construye filas de formulario según el esquema de propiedades de cada diseño. */
object FormBuilder {

    private val SWATCHES = listOf(
        0xFF6750A4.toInt(), 0xFF2196F3.toInt(), 0xFF009688.toInt(), 0xFF4CAF50.toInt(),
        0xFF8BC34A.toInt(), 0xFFFFC107.toInt(), 0xFFFF9800.toInt(), 0xFFF44336.toInt(),
        0xFFE91E63.toInt(), 0xFFFFFFFF.toInt(), 0xFF9E9E9E.toInt(), 0xFF1B1B1F.toInt(),
    )

    private val SWATCH_NAMES = listOf(
        "morado", "azul", "turquesa", "verde", "lima", "ámbar",
        "naranja", "rojo", "rosa", "blanco", "gris", "negro",
    )

    fun build(activity: EditorActivity, props: List<Designs.Prop>, values: JSONObject, host: LinearLayout, onChange: () -> Unit) {
        host.removeAllViews()
        val d = activity.resources.displayMetrics.density
        for (p in props) {
            val resId = activity.resources.getIdentifier(p.labelRes, "string", activity.packageName)
            val label = TextView(activity).apply {
                text = if (resId != 0) activity.getString(resId) else Designs.labelFor(p)
                textSize = 13f
                setPadding(0, (14 * d).toInt(), 0, (4 * d).toInt())
            }
            host.addView(label)
            when (p.type) {
                Designs.Type.TEXT, Designs.Type.MULTILINE -> {
                    val edit = EditText(activity).apply {
                        setText(values.optString(p.key, ""))
                        inputType = if (p.type == Designs.Type.MULTILINE)
                            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        else android.text.InputType.TYPE_CLASS_TEXT
                        maxLines = if (p.type == Designs.Type.MULTILINE) 4 else 1
                        textSize = 14f
                    }
                    edit.addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            values.put(p.key, s?.toString() ?: "")
                            onChange()
                        }
                    })
                    host.addView(edit)
                }
                Designs.Type.NUMBER -> {
                    val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                    val valueLabel = TextView(activity).apply {
                        textSize = 13f; setPadding((8 * d).toInt(), 0, 0, 0)
                        text = values.optInt(p.key, p.range?.first ?: 0).toString()
                    }
                    val bar = SeekBar(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        p.range?.let { min = it.first; max = it.last }
                        progress = values.optInt(p.key, p.range?.first ?: 0).coerceIn(p.range?.first ?: 0, p.range?.last ?: 100)
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                                valueLabel.text = progress.toString()
                            }
                            override fun onStartTrackingTouch(sb: SeekBar?) {}
                            override fun onStopTrackingTouch(sb: SeekBar?) {
                                values.put(p.key, sb?.progress ?: 0)
                                onChange()
                            }
                        })
                    }
                    row.addView(bar); row.addView(valueLabel)
                    host.addView(row)
                }
                Designs.Type.SWITCH -> {
                    val sw = SwitchCompat(activity).apply {
                        isChecked = values.optBoolean(p.key, false)
                        text = if (isChecked) "Sí" else "No"
                        textSize = 14f
                        setOnCheckedChangeListener { _, checked ->
                            values.put(p.key, checked)
                            text = if (checked) "Sí" else "No"
                            onChange()
                        }
                    }
                    host.addView(sw)
                }
                Designs.Type.COLOR -> {
                    host.addView(colorRow(activity, p, values, d, onChange))
                }
                Designs.Type.DATETIME -> {
                    val btn = Button(activity).apply {
                        textSize = 13f
                        isAllCaps = false
                        text = StyleEngine.formatDateTime(values.optLong(p.key, System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
                        setOnClickListener {
                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = values.optLong(p.key, System.currentTimeMillis()) }
                            DatePickerDialog(activity, { _, y, m, day ->
                                TimePickerDialog(activity, { _, hh, mm ->
                                    cal.set(y, m, day, hh, mm, 0)
                                    values.put(p.key, cal.timeInMillis)
                                    text = StyleEngine.formatDateTime(cal.timeInMillis)
                                    onChange()
                                }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
                            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
                        }
                    }
                    host.addView(btn)
                }
                Designs.Type.APPS -> {
                    host.addView(appsRow(activity, p, values, d, onChange))
                }
                Designs.Type.OPTION -> {
                    val spinner = Spinner(activity)
                    spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, p.options)
                    val current = values.optString(p.key, p.options.firstOrNull() ?: "")
                    spinner.setSelection(p.options.indexOf(current).coerceAtLeast(0))
                    spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            values.put(p.key, p.options[pos])
                            onChange()
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                    host.addView(spinner)
                }
            }
        }
    }

    private fun colorRow(
        activity: EditorActivity, p: Designs.Prop, values: JSONObject, d: Float, onChange: () -> Unit,
    ): View {
        val swatchHost = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        var chosen = values.optString(p.key, StyleEngine.AUTO)
        val autoVisible = p.allowAuto
        val children = mutableListOf<View>()

        fun redraw() {
            for (i in children.indices) {
                val isAutoChip = autoVisible && i == 0
                val swatchIdx = i - (if (autoVisible) 1 else 0)
                val color = SWATCHES.getOrNull(swatchIdx)
                val selected = if (isAutoChip) chosen == StyleEngine.AUTO
                    else color != null && chosen.equals(String.format("#%06X", color and 0xFFFFFF), true)
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (isAutoChip) {
                        setColor(Color.TRANSPARENT)
                        setStroke((2 * d).toInt(), if (selected) 0xFF6750A4.toInt() else 0x669E9E9E)
                    } else {
                        setColor(color ?: Color.GRAY)
                        setStroke((if (selected) 4 else 1) * d.toInt().coerceAtLeast(1),
                            if (selected) 0xFF6750A4.toInt() else 0x33FFFFFF)
                    }
                }
                children[i].background = bg
            }
        }

        fun setChosen(v: String) {
            chosen = v
            values.put(p.key, v)
            redraw()
            onChange()
        }

        if (autoVisible) {
            val chip = TextView(activity).apply {
                text = "Auto"
                textSize = 12f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams((54 * d).toInt(), (28 * d).toInt()).apply { marginEnd = (6 * d).toInt() }
                contentDescription = activity.getString(R.string.style_bg_auto)
                setOnClickListener { setChosen(StyleEngine.AUTO) }
            }
            children.add(chip)
            swatchHost.addView(chip)
        }
        SWATCHES.forEachIndexed { i, color ->
            val v = View(activity).apply {
                layoutParams = LinearLayout.LayoutParams((28 * d).toInt(), (28 * d).toInt()).apply { marginEnd = (6 * d).toInt() }
                contentDescription = SWATCH_NAMES[i]
                setOnClickListener { setChosen(String.format("#%06X", color and 0xFFFFFF)) }
            }
            children.add(v)
            swatchHost.addView(v)
        }
        redraw()
        return swatchHost
    }

    private fun appsRow(activity: EditorActivity, p: Designs.Prop, values: JSONObject, d: Float, onChange: () -> Unit): View {
        val col = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val info = TextView(activity).apply { textSize = 12f; alpha = 0.8f }
        val btn = Button(activity).apply {
            textSize = 13f; isAllCaps = false; text = activity.getString(R.string.choose_app)
        }
        fun refreshInfo() {
            val arr = values.optJSONArray(p.key) ?: JSONArray()
            val names = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("label") }
            info.text = if (names.isEmpty()) "Ninguna elegida" else names.joinToString(", ")
        }
        refreshInfo()
        btn.setOnClickListener {
            val pm = activity.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
                .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
                .distinctBy { it.second }
                .sortedBy { it.first }
            val arr = values.optJSONArray(p.key) ?: JSONArray()
            val chosen = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("pkg") }.toMutableList()
            val labels = apps.map { it.first }.toTypedArray()
            val checked = apps.map { it.second in chosen }.toBooleanArray()
            AlertDialog.Builder(activity)
                .setTitle(R.string.choose_app)
                .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                    val pkg = apps[which].second
                    if (isChecked) {
                        if (chosen.size >= 4) checked[which] = false
                        else chosen.add(pkg)
                    } else chosen.remove(pkg)
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val newArr = JSONArray()
                    for (pkg in chosen) {
                        val label = apps.firstOrNull { it.second == pkg }?.first ?: pkg
                        newArr.put(JSONObject().put("pkg", pkg).put("label", label))
                    }
                    values.put(p.key, newArr)
                    refreshInfo()
                    onChange()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        col.addView(btn)
        col.addView(info)
        return col
    }
}
