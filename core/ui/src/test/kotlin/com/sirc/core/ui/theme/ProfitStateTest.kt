package com.sirc.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.sirc.domain.model.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfitStateTest {
    @Test
    fun `fromDecision mapea cada decision a su estado`() {
        assertEquals(ProfitState.PROFITABLE, ProfitState.fromDecision(Decision.PROFITABLE))
        assertEquals(ProfitState.MARGINAL, ProfitState.fromDecision(Decision.MARGINAL))
        assertEquals(ProfitState.NOT_PROFITABLE, ProfitState.fromDecision(Decision.NOT_PROFITABLE))
    }

    @Test
    fun `etiquetas de semaforo son las esperadas`() {
        assertEquals("CONVIENE", ProfitState.PROFITABLE.label)
        assertEquals("DUDOSO", ProfitState.MARGINAL.label)
        assertEquals("NO CONVIENE", ProfitState.NOT_PROFITABLE.label)
    }

    @Test
    fun `colores coinciden con la paleta SIRC`() {
        assertEquals(SircColors.Profit, ProfitState.PROFITABLE.color)
        assertEquals(SircColors.Marginal, ProfitState.MARGINAL.color)
        assertEquals(SircColors.NotProfit, ProfitState.NOT_PROFITABLE.color)
    }
}

class SircColorsTest {
    @Test
    fun `paleta de semaforo usa los valores exactos`() {
        assertEquals(Color(0xFF1DB954), SircColors.Profit)
        assertEquals(Color(0xFFF5A623), SircColors.Marginal)
        assertEquals(Color(0xFFE5484D), SircColors.NotProfit)
        assertEquals(Color(0xFF2A8CFF), SircColors.Accent)
    }

    @Test
    fun `fondo y borde del overlay mantienen transparencia`() {
        assertEquals(0xE6, (SircColors.OverlayBackground.alpha * 255).toInt())
        assertEquals(0x33, (SircColors.OverlayBorder.alpha * 255).toInt())
    }
}
