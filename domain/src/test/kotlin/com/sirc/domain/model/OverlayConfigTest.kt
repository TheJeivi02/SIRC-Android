package com.sirc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayConfigTest {
    @Test
    fun `activeIndicatorCount excluye compactMode`() {
        assertEquals(
            OverlayConfig(compactMode = false).activeIndicatorCount,
            OverlayConfig(compactMode = true).activeIndicatorCount,
        )
    }

    @Test
    fun `activeIndicatorCount cuenta solo los indicadores activos`() {
        val config = OverlayConfig(showProfit = false, showProfitPerKm = false)
        assertEquals(3, config.activeIndicatorCount)
    }

    @Test
    fun `activeIndicatorCount por defecto es cuatro`() {
        assertEquals(4, OverlayConfig().activeIndicatorCount)
    }
}
