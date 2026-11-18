package com.widgetcraft.app.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Padding de barras del sistema y teclado. Con targetSdk 35 Android 15 fuerza
 * edge-to-edge: sin esto la status bar tapa los botones y la barra de gestos
 * el final del formulario. En dispositivos sin edge-to-edge los insets llegan
 * consumidos y el helper no hace nada (no duplica padding).
 */
fun applySystemBarInsets(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
        WindowInsetsCompat.CONSUMED
    }
}
