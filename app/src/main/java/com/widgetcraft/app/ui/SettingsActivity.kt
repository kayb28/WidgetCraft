package com.widgetcraft.app.ui

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.widgetcraft.app.BuildConfig
import com.widgetcraft.app.R
import com.widgetcraft.app.core.WidgetStore

/** Ajustes: tema claro/oscuro/sistema (los widgets ya se adaptan solos al modo del sistema). */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle(R.string.settings_title)

        val group = findViewById<RadioGroup>(R.id.theme_group)
        when (WidgetStore.themeMode(this)) {
            "light" -> group.check(R.id.theme_light)
            "dark" -> group.check(R.id.theme_dark)
            else -> group.check(R.id.theme_system)
        }
        group.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.theme_light -> "light"
                R.id.theme_dark -> "dark"
                else -> "system"
            }
            WidgetStore.setThemeMode(this, mode)
            when (mode) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        findViewById<TextView>(R.id.about_text).text =
            "WidgetCraft ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})\n" +
            "github.com/kayb28/WidgetCraft\n" +
            "Certificado de firma: CN=WidgetCraft"
    }
}
