package com.widgetcraft.app.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El catálogo completo debe producir configuraciones válidas y redond-trip en JSON. */
class DesignsTest {

    @Test
    fun `hay exactamente 12 diseños en el catálogo`() {
        assertEquals(12, Designs.all.size)
        assertEquals(12, Designs.all.map { it.id }.distinct().size)
    }

    @Test
    fun `defaultConfig de cada diseño incluye todas sus propiedades y el estilo global`() {
        for (design in Designs.all) {
            val cfg = Designs.defaultConfig(design.id)
            assertEquals(design.id, cfg.getString("design"))
            val props = cfg.getJSONObject("props")
            for (p in design.props) {
                assertTrue("${design.id}.${p.key} falta", props.has(p.key))
            }
            val style = cfg.getJSONObject("style")
            for (sp in Designs.styleProps) {
                assertTrue("${design.id} estilo ${sp.key} falta", style.has(sp.key))
            }
            // round-trip
            val copy = JSONObject(cfg.toString())
            assertEquals(cfg.getString("id"), copy.getString("id"))
            assertEquals(design.id, copy.getString("design"))
        }
    }

    @Test
    fun `la cuenta atrás por defecto está en el futuro`() {
        val cfg = Designs.defaultConfig("countdown")
        val target = cfg.getJSONObject("props").getLong("objetivo")
        assertTrue(target > System.currentTimeMillis())
    }

    @Test
    fun `los accesos directos nacen vacíos`() {
        val cfg = Designs.defaultConfig("shortcuts")
        assertEquals(0, cfg.getJSONObject("props").getJSONArray("apps").length())
    }

    @Test
    fun `el estilo por defecto es auto y se adapta al modo`() {
        val style = Designs.defaultConfig("clock").getJSONObject("style")
        assertEquals("auto", style.getString(Designs.STYLE_BG_COLOR))
        assertEquals("auto", style.getString(Designs.STYLE_TEXT_COLOR))
    }
}
