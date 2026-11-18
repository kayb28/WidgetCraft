package com.widgetcraft.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.widgetcraft.app.R
import com.widgetcraft.app.core.Designs
import com.widgetcraft.app.core.WidgetStore
import com.widgetcraft.app.render.WidgetRenderer
import com.widgetcraft.app.widget.WidgetProvider

/** Pantalla principal: biblioteca de configuraciones + catálogo de 12 diseños. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageView>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.btn_pin).setOnClickListener { requestPinWidget() }

        rebuild()
    }

    override fun onResume() {
        super.onResume()
        rebuild()
    }

    private fun applyTheme() {
        when (WidgetStore.themeMode(this)) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun rebuild() {
        val libHost = findViewById<LinearLayout>(R.id.library_host)
        val empty = findViewById<TextView>(R.id.empty_library)
        libHost.removeAllViews()
        val lib = WidgetStore.library(this)
        empty.visibility = if (lib.isEmpty()) View.VISIBLE else View.GONE
        for (cfg in lib) libHost.addView(libraryRow(cfg))

        val gridHost = findViewById<LinearLayout>(R.id.catalog_host)
        gridHost.removeAllViews()
        var row: LinearLayout? = null
        for ((i, design) in Designs.all.withIndex()) {
            if (i % 2 == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                }
                gridHost.addView(row)
            }
            row?.addView(catalogCard(design))
        }
    }

    private fun libraryRow(cfg: org.json.JSONObject): View {
        val d = resources.displayMetrics.density
        val design = Designs.byId(cfg.optString("design", "clock"))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * d).toInt()
            }
            setBackgroundResource(R.drawable.card_bg)
            setPadding((10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt())
        }
        // mini preview real (RemoteViews aplicado a un contenedor pequeño)
        val previewHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((72 * d).toInt(), (48 * d).toInt())
        }
        try {
            val rv = WidgetRenderer.render(this, cfg, (72 * d).toInt(), (48 * d).toInt(), -1)
            previewHost.addView(rv.apply(this, previewHost))
        } catch (_: Exception) {
            val fallback = TextView(this).apply { text = "▦" ; textSize = 24f }
            previewHost.addView(fallback)
        }
        root.addView(previewHost)

        val name = TextView(this).apply {
            text = getString(design.titleRes)
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (10 * d).toInt()
            }
        }
        root.addView(name)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((icon, desc, action) in listOf(
            Triple(R.drawable.ic_gear, getString(R.string.edit), { editConfig(cfg.getString("id")) }),
            Triple(android.R.drawable.ic_menu_manage, getString(R.string.apply_to_widget), { applyToWidget(cfg.getString("id")) }),
            Triple(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.delete), { deleteConfig(cfg.getString("id")) }),
        )) {
            actions.addView(ImageView(this).apply {
                setImageResource(icon)
                setColorFilter(ContextCompat.getColor(context, R.color.md_primary))
                layoutParams = LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt()).apply { marginStart = (6 * d).toInt() }
                setPadding((4 * d).toInt(), (4 * d).toInt(), (4 * d).toInt(), (4 * d).toInt())
                contentDescription = desc
                setOnClickListener { action() }
            })
        }
        root.addView(actions)
        root.setOnClickListener { editConfig(cfg.getString("id")) }
        return root
    }

    private fun catalogCard(design: Designs.Design): View {
        val d = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = if (Designs.all.indexOf(design) % 2 == 0) (8 * d).toInt() else 0
            }
            setBackgroundResource(R.drawable.card_bg)
            setPadding((12 * d).toInt(), (16 * d).toInt(), (12 * d).toInt(), (16 * d).toInt())
        }
        val iconRes = resources.getIdentifier(design.iconRes, "drawable", packageName)
        card.addView(ImageView(this).apply {
            setImageResource(if (iconRes != 0) iconRes else R.drawable.ic_gear)
            layoutParams = LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt())
        })
        card.addView(TextView(this).apply {
            text = getString(design.titleRes)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, (8 * d).toInt(), 0, 0)
        })
        card.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
                .putExtra(EditorActivity.EXTRA_DESIGN_ID, design.id)
            startActivity(intent)
        }
        return card
    }

    private fun editConfig(configId: String) {
        startActivity(Intent(this, EditorActivity::class.java)
            .putExtra(EditorActivity.EXTRA_CONFIG_ID, configId))
    }

    private fun deleteConfig(configId: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                WidgetStore.deleteLibraryEntry(this, configId)
                WidgetRenderer.pushAll(this)
                rebuild()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun applyToWidget(configId: String) {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(WidgetProvider.component(this)).toList()
        if (ids.isEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(R.string.no_installed_widgets)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val labels = ids.map { "#$it" }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.choose_widget)
            .setItems(labels) { _, which ->
                WidgetStore.assignWidget(this, configId, ids[which])
                WidgetRenderer.pushWidget(this, ids[which])
                rebuild()
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setMessage(R.string.applied_to_widget)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun requestPinWidget() {
        val mgr = AppWidgetManager.getInstance(this)
        if (!mgr.isRequestPinAppWidgetSupported) return
        mgr.requestPinAppWidget(WidgetProvider.component(this), null, null)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(R.string.pin_requested)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
