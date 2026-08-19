package com.sirc.domain.engine

import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitEngineTest {
    private val engine = ProfitEngine()

    private val costs = DriverCosts(costPerKm = 2.0, costPerTrip = 1.0)
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
        costs: DriverCosts = this.costs,
        thresholds: DecisionThresholds = this.thresholds,
    ) = engine.evaluate(offer(total, distanceKm, durationMin), costs, thresholds)

    @Test
    fun `oferta clara y rentable produce PROFITABLE`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 30.0)

        // Costo = fijo 1 + 10*2 = 21 (sin costo por minuto).
        assertEquals(21.0, evaluation.metrics.totalCost, 0.001)
        assertEquals(79.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(7.9, evaluation.metrics.profitPerKm ?: -1.0, 0.001)
        assertEquals(158.0, evaluation.metrics.profitPerHour ?: -1.0, 0.001)
        assertEquals(Decision.PROFITABLE, evaluation.decision)
    }

    @Test
    fun `oferta perdedora produce NOT_PROFITABLE`() {
        val evaluation = evaluate(total = 15.0, distanceKm = 10.0, durationMin = 30.0)

        assertEquals(Decision.NOT_PROFITABLE, evaluation.decision)
        assertTrue(evaluation.metrics.estimatedProfit < 0.0)
    }

    @Test
    fun `ganancia positiva pero bajo umbral horario produce MARGINAL`() {
        // Ganancia 79 pero en 45 min => ~105.3/h < 120.
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 45.0)

        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `ganancia por km bajo el umbral produce MARGINAL`() {
        // Costo = 1 + 40*2 = 81; ganancia = 200-81 = 119; /km = 2.975 < 4.
        val evaluation = evaluate(total = 200.0, distanceKm = 40.0, durationMin = 30.0)

        assertEquals(119.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(2.975, evaluation.metrics.profitPerKm ?: -1.0, 0.001)
        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `oferta sin distancia ni duracion no es evaluable`() {
        val result = runCatching { evaluate(total = 100.0, distanceKm = 0.0, durationMin = 0.0) }
        assertTrue(result.isFailure)
    }

    @Test
    fun `caso real 590 en 27 minutos sin distancia no es perdida`() {
        // Referencia de auditoría: costo/km 0.50, costo fijo $0, objetivo $11/h.
        val ecuadorCosts = DriverCosts(costPerKm = 0.5, costPerTrip = 0.0)
        val ecuadorTarget = DecisionThresholds(minProfitPerKm = 0.5, minProfitPerHour = 11.0)
        val evaluation =
            evaluate(
                total = 5.90,
                distanceKm = 0.0,
                durationMin = 27.0,
                costs = ecuadorCosts,
                thresholds = ecuadorTarget,
            )

        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.metrics.estimatedProfit >= 0.0)
        assertNull(evaluation.metrics.profitPerKm)
        assertNull(evaluation.metrics.profitPerHour)
        assertTrue(evaluation.reasons.any { it.contains("no confirmable sin distancia") })
    }

    @Test
    fun `distancia desconocida nunca produce ACCEPT`() {
        // Ganancia de mejor caso muy alta, pero sin distancia no se confirma.
        val evaluation = evaluate(total = 100.0, distanceKm = 0.0, durationMin = 30.0)

        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertNull(evaluation.metrics.profitPerKm)
        assertNull(evaluation.metrics.profitPerHour)
    }

    @Test
    fun `duracion desconocida no fabrica ganancia por hora`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 0.0)

        assertNull(evaluation.metrics.profitPerHour)
        assertEquals(7.9, evaluation.metrics.profitPerKm ?: -1.0, 0.001)
        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `ganancia cero no se trata como perdida`() {
        // Costo = 1 + 10*2 = 21 == ingreso -> break-even.
        val evaluation = evaluate(total = 21.0, distanceKm = 10.0, durationMin = 30.0)

        assertEquals(0.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.reasons.any { it.contains("solo cubre los costos") })
    }

    @Test
    fun `costo fijo afecta la ganancia real pero no el objetivo`() {
        val sinFijo = DriverCosts(costPerKm = 2.0, costPerTrip = 0.0)
        val conFijo = DriverCosts(costPerKm = 2.0, costPerTrip = 60.0)

        val sinFijoDecision = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 30.0, costs = sinFijo).decision
        val conFijoDecision = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 30.0, costs = conFijo).decision

        // Sin fijo: ganancia 80, 160/h y 8/km -> cumple. Con fijo: ganancia 20, 40/h -> no cumple.
        assertEquals(Decision.PROFITABLE, sinFijoDecision)
        assertEquals(Decision.MARGINAL, conFijoDecision)
    }

    @Test
    fun `formato de moneda usa simbolo por codigo`() {
        assertEquals("$100.5", engine.formatCurrency(100.5, "MXN"))
        assertEquals("R$40", engine.formatCurrency(40.0, "BRL"))
    }
}
