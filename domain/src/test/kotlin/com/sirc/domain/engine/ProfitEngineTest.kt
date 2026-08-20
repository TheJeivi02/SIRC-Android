package com.sirc.domain.engine

import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.GoalStatus
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
    fun `oferta clara y rentable produce PROFITABLE con todas las metas en verde`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 30.0)

        // Costo = fijo 1 + 10*2 = 21 (sin costo por minuto).
        assertEquals(21.0, evaluation.metrics.totalCost, 0.001)
        assertEquals(79.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(7.9, evaluation.metrics.profitPerKm ?: -1.0, 0.001)
        assertEquals(158.0, evaluation.metrics.profitPerHour ?: -1.0, 0.001)
        assertEquals(GoalStatus.MET, evaluation.metrics.netGoal)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerKmGoal)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerHourGoal)
        assertEquals(Decision.PROFITABLE, evaluation.decision)
    }

    @Test
    fun `oferta perdedora produce NOT_PROFITABLE`() {
        val evaluation = evaluate(total = 15.0, distanceKm = 10.0, durationMin = 30.0)

        assertEquals(Decision.NOT_PROFITABLE, evaluation.decision)
        assertTrue(evaluation.metrics.estimatedProfit < 0.0)
    }

    @Test
    fun `perdida real marca todas las metricas en rojo`() {
        val evaluation = evaluate(total = 15.0, distanceKm = 10.0, durationMin = 30.0)

        assertEquals(GoalStatus.FAILED, evaluation.metrics.netGoal)
        assertEquals(GoalStatus.FAILED, evaluation.metrics.profitPerKmGoal)
        assertEquals(GoalStatus.FAILED, evaluation.metrics.profitPerHourGoal)
    }

    @Test
    fun `ganancia positiva pero bajo umbral horario produce MARGINAL`() {
        // Ganancia 79 pero en 45 min => ~105.3/h < 120.
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 45.0)

        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertEquals(GoalStatus.NEAR, evaluation.metrics.profitPerHourGoal)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerKmGoal)
        assertTrue(evaluation.reasons.any { it.contains("Ganancia/hora menor al objetivo") })
    }

    @Test
    fun `ganancia por km bajo el umbral produce MARGINAL`() {
        // Costo = 1 + 40*2 = 81; ganancia = 200-81 = 119; /km = 2.975 < 4.
        val evaluation = evaluate(total = 200.0, distanceKm = 40.0, durationMin = 30.0)

        assertEquals(119.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(2.975, evaluation.metrics.profitPerKm ?: -1.0, 0.001)
        assertEquals(GoalStatus.NEAR, evaluation.metrics.profitPerKmGoal)
        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `solo duracion calcula ganancia por hora sin inventar distancia`() {
        // Sin distancia no hay costo de distancia: totalCost = fijo 1.
        val evaluation = evaluate(total = 100.0, distanceKm = 0.0, durationMin = 30.0)

        assertEquals(1.0, evaluation.metrics.totalCost, 0.001)
        assertEquals(99.0, evaluation.metrics.estimatedProfit, 0.001)
        assertNull(evaluation.metrics.profitPerKm)
        assertNull(evaluation.metrics.profitPerKmGoal)
        assertEquals(198.0, evaluation.metrics.profitPerHour ?: -1.0, 0.001)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerHourGoal)
        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.reasons.any { it.contains("Cumple el objetivo por hora") })
        assertTrue(evaluation.reasons.any { it.contains("Falta la distancia") })
    }

    @Test
    fun `solo duracion nunca produce ACCEPT pese a cumplir el objetivo horario`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 0.0, durationMin = 30.0)

        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerHourGoal)
    }

    @Test
    fun `solo distancia calcula ganancia por km sin inventar duracion`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 0.0)

        assertEquals(21.0, evaluation.metrics.totalCost, 0.001)
        assertEquals(7.9, evaluation.metrics.profitPerKm ?: -1.0, 0.001)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerKmGoal)
        assertNull(evaluation.metrics.profitPerHour)
        assertNull(evaluation.metrics.profitPerHourGoal)
        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.reasons.any { it.contains("Cumple el objetivo por km") })
        assertTrue(evaluation.reasons.any { it.contains("Falta la duración") })
    }

    @Test
    fun `solo duracion con objetivo horario incumplido es NEAR`() {
        // Ganancia 59 en 30 min => 118/h < 120, pero positiva.
        val evaluation = evaluate(total = 60.0, distanceKm = 0.0, durationMin = 30.0)

        assertEquals(GoalStatus.NEAR, evaluation.metrics.profitPerHourGoal)
        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.reasons.any { it.contains("Ganancia/hora menor al objetivo") })
    }

    @Test
    fun `precio sin distancia ni duracion se evalua sin inventar metricas`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 0.0, durationMin = 0.0)

        assertEquals(99.0, evaluation.metrics.estimatedProfit, 0.001)
        assertNull(evaluation.metrics.profitPerKm)
        assertNull(evaluation.metrics.profitPerHour)
        assertEquals(GoalStatus.MET, evaluation.metrics.netGoal)
        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.reasons.any { it.contains("Faltan la distancia y la duración") })
    }

    @Test
    fun `ejemplo obligatorio 6 dolares en 30 minutos sin distancia cumple el objetivo horario`() {
        val ecuadorCosts = DriverCosts(costPerKm = 0.5, costPerTrip = 0.0)
        val ecuadorTarget = DecisionThresholds(minProfitPerKm = 0.5, minProfitPerHour = 11.0)
        val evaluation =
            evaluate(
                total = 6.0,
                distanceKm = 0.0,
                durationMin = 30.0,
                costs = ecuadorCosts,
                thresholds = ecuadorTarget,
            )

        assertEquals(6.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(12.0, evaluation.metrics.profitPerHour ?: -1.0, 0.001)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerHourGoal)
        assertNull(evaluation.metrics.profitPerKm)
        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `caso real 590 en 27 minutos sin distancia calcula ganancia por hora`() {
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
        assertEquals(5.9 / 0.45, evaluation.metrics.profitPerHour ?: -1.0, 0.001)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerHourGoal)
    }

    @Test
    fun `la evaluacion usa el objetivo por hora configurado y no un valor fijo`() {
        // Misma oferta (ganancia 13.0 -> 13/h): MET con objetivo 11, por debajo con 15.
        val ecuadorCosts = DriverCosts(costPerKm = 0.5, costPerTrip = 0.0)
        val conObjetivoOnce =
            evaluate(
                total = 14.0,
                distanceKm = 2.0,
                durationMin = 60.0,
                costs = ecuadorCosts,
                thresholds = DecisionThresholds(minProfitPerKm = 0.5, minProfitPerHour = 11.0),
            )
        val conObjetivoQuince =
            evaluate(
                total = 14.0,
                distanceKm = 2.0,
                durationMin = 60.0,
                costs = ecuadorCosts,
                thresholds = DecisionThresholds(minProfitPerKm = 0.5, minProfitPerHour = 15.0),
            )

        assertEquals(13.0, conObjetivoOnce.metrics.profitPerHour ?: -1.0, 0.001)
        assertEquals(GoalStatus.MET, conObjetivoOnce.metrics.profitPerHourGoal)
        assertEquals(GoalStatus.NEAR, conObjetivoQuince.metrics.profitPerHourGoal)
    }

    @Test
    fun `distancia desconocida nunca produce ACCEPT pese a cumplir el objetivo horario`() {
        // Ganancia de mejor caso muy alta, pero sin distancia no se confirma.
        val evaluation = evaluate(total = 100.0, distanceKm = 0.0, durationMin = 30.0)

        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertNull(evaluation.metrics.profitPerKm)
        assertEquals(198.0, evaluation.metrics.profitPerHour ?: -1.0, 0.001)
    }

    @Test
    fun `duracion desconocida no fabrica ganancia por hora`() {
        val evaluation = evaluate(total = 100.0, distanceKm = 10.0, durationMin = 0.0)

        assertNull(evaluation.metrics.profitPerHour)
        assertEquals(7.9, evaluation.metrics.profitPerKm ?: -1.0, 0.001)
        assertEquals(GoalStatus.MET, evaluation.metrics.profitPerKmGoal)
        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `ganancia cero no se trata como perdida`() {
        // Costo = 1 + 10*2 = 21 == ingreso -> break-even.
        val evaluation = evaluate(total = 21.0, distanceKm = 10.0, durationMin = 30.0)

        assertEquals(0.0, evaluation.metrics.estimatedProfit, 0.001)
        assertEquals(GoalStatus.NEAR, evaluation.metrics.netGoal)
        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.reasons.any { it.contains("solo cubre los costos") })
    }

    @Test
    fun `oferta sin monto no es evaluable`() {
        val offer =
            TripOffer(
                platform = RidePlatform.UBER,
                timestampMillis = 1_700_000_000_000,
                estimatedTotal = null,
                fareAmount = null,
                distanceKm = 10.0,
                durationMin = 30.0,
                currency = "MXN",
            )

        val result = runCatching { engine.evaluate(offer, costs, thresholds) }

        assertTrue(result.isFailure)
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
