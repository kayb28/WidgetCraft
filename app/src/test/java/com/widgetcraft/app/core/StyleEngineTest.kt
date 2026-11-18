package com.widgetcraft.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StyleEngineTest {

    private fun cal(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }

    @Test
    fun `progreso del día arranca en cero y cierra en cien`() {
        val start = StyleEngine.periodProgress("dia", cal(2026, 11, 18, 0, 0))
        val end = StyleEngine.periodProgress("dia", cal(2026, 11, 18, 23, 59))
        assertTrue(start is StyleEngine.Period.Running)
        assertEquals(0, (start as StyleEngine.Period.Running).percent)
        assertTrue((end as StyleEngine.Period.Running).percent in 90..100)
    }

    @Test
    fun `progreso del mes a mitad de mes ronda la mitad`() {
        val mid = StyleEngine.periodProgress("mes", cal(2026, 11, 15, 12, 0))
        assertTrue((mid as StyleEngine.Period.Running).percent in 45..55)
    }

    @Test
    fun `progreso del año nunca se sale de 0 a 100`() {
        for (month in 1..12) {
            val p = StyleEngine.periodProgress("ano", cal(2026, month, 15, 12, 0))
            assertTrue((p as StyleEngine.Period.Running).percent in 0..100)
        }
    }

    @Test
    fun `cuenta atrás futura da días y horas`() {
        val now = cal(2026, 11, 18, 10, 0).timeInMillis
        val target = cal(2026, 11, 25, 10, 0).timeInMillis
        val r = StyleEngine.countdown(target, now)
        assertTrue(r is StyleEngine.Period.Running)
        assertEquals("7d 0h", (r as StyleEngine.Period.Running).label)
    }

    @Test
    fun `cuenta atrás pasada devuelve Done`() {
        val now = cal(2026, 11, 18, 10, 0).timeInMillis
        val past = cal(2026, 11, 1, 10, 0).timeInMillis
        assertTrue(StyleEngine.countdown(past, now) is StyleEngine.Period.Done)
    }

    @Test
    fun `formatElapsed compone días horas minutos y segundos`() {
        assertEquals("00:00:00", StyleEngine.formatElapsed(0))
        assertEquals("01:01:01", StyleEngine.formatElapsed(3661_000L))
        assertEquals("1d 00:00:01", StyleEngine.formatElapsed(86401_000L))
        assertEquals("00:00:00", StyleEngine.formatElapsed(-5_000L))
    }

    @Test
    fun `formatDateTime devuelve formato español abreviado`() {
        val s = StyleEngine.formatDateTime(cal(2026, 12, 25, 9, 5).timeInMillis)
        assertTrue(s.contains("25 dic"))
        assertTrue(s.contains("09:05"))
    }

    @Test
    fun `formatElapsed con milisegundos negativos no explota`() {
        assertEquals("00:00:00", StyleEngine.formatElapsed(-1L))
    }
}
