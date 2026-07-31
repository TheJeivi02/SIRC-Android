package com.sirc.domain.engine

import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitEngineTest {
    private val engine = ProfitEngine()

    private val costs =
        DriverCosts(
            costPerKm = 2.0,
            costPerMinute = 0.3,
            costPerTrip = 1.0,
        )

    private val thresholds = DecisionThresholds(minProfitPerKm = 4.0, minProfitPerHour = 120.0)

    private fun offer(
        total: Double,
        distanceKm: Double,
        durationMin: Double,
    ) = TripOffer(
        platform = RidePlatform.UBER,
        timestampMillis = 1_700_000_000_000,
        estimatedTotal = total,
        distanceKm = distanceKm,
        durationMin = durationMin,
        currency = "MXN",
    )

    private fun evaluate(
        total: Double,
        distanceKm: Double,
        durationMin: Double,
    ) = engine.evaluate(offer(total, distanceKm, durationMin), costs, thresholds)

    @Test
    fun `oferta clara y rentable produce PROFITABLE`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 30.0)

        // Costo = 1 + 10*2 + 30*0.3 = 1 + 20 + 9 = 30
        assertEquals(30.0, evaluation.metrics.totalCost, 0.001)
        // Ganancia = 100 - 30 = 70
        assertEquals(70.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(7.0, evaluation.metrics.profitPerKm, 0.001)
        assertEquals(140.0, evaluation.metrics.profitPerHour, 0.001)
        assertEquals(Decision.PROFITABLE, evaluation.decision)
    }

    @Test
    fun `oferta perdedora produce NOT_PROFITABLE`() {
        val evaluation = evaluate(total = 25.0, distanceKm = 10.0, durationMin = 30.0)

        assertEquals(Decision.NOT_PROFITABLE, evaluation.decision)
        assertTrue(evaluation.metrics.estimatedProfit < 0.0)
    }

    @Test
    fun `ganancia positiva pero bajo umbral horario produce MARGINAL`() {
        // Ganancia 70 pero en 45 min => 93.3/hr < 120
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 45.0)

        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `ganancia por km bajo el umbral produce MARGINAL`() {
        // Costo = 1 + 40*2 + 30*0.3 = 90; ganancia = 200-90 = 110
        // Ganancia/hora = 220 >= 120, pero ganancia/km = 2.75 < 4
        val evaluation = evaluate(total = 200.0, distanceKm = 40.0, durationMin = 30.0)

        assertEquals(110.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(2.75, evaluation.metrics.profitPerKm, 0.001)
        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `oferta sin distancia ni duracion no es evaluable`() {
        val result = runCatching { evaluate(total = 100.0, distanceKm = 0.0, durationMin = 0.0) }
        assertTrue(result.isFailure)
    }

    @Test
    fun `formato de moneda usa simbolo por codigo`() {
        assertEquals("$100.5", engine.formatCurrency(100.5, "MXN"))
        assertEquals("R$40", engine.formatCurrency(40.0, "BRL"))
    }
}
