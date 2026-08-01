package com.sirc.domain.engine

import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.RuleEvaluation
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleVerdict
import com.sirc.domain.model.TripOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceEngineTest {
    private val engine = ConfidenceEngine()

    @Test
    fun `oferta completa y coherente da confianza alta y accionable`() {
        val result = engine.assess(offer(), metrics())

        assertEquals(ConfidenceLevel.HIGH, result.level)
        assertTrue(result.isActionable)
        assertTrue(result.percent >= 80)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun `oferta sin datos suficientes es LOW y no accionable`() {
        val incomplete =
            TripOffer(
                platform = RidePlatform.UBER,
                timestampMillis = 1_700_000_000_000,
                estimatedTotal = 50.0,
                distanceKm = null,
                durationMin = null,
            )

        val result = engine.assess(incomplete, metrics())

        assertEquals(ConfidenceLevel.LOW, result.level)
        assertFalse(result.isActionable)
        assertTrue(result.reasons.isNotEmpty())
    }

    @Test
    fun `metricas incoherentes degradan a LOW`() {
        val crazy =
            ProfitMetrics(
                estimatedTotal = 100.0,
                distanceKm = 1.0,
                durationMin = 5.0,
                totalCost = 40.0,
                estimatedProfit = 60.0,
                profitPerKm = 900.0,
                profitPerHour = 720.0,
                marginPercent = 60.0,
            )

        val result = engine.assess(offer(), crazy)

        assertEquals(ConfidenceLevel.LOW, result.level)
        assertFalse(result.isActionable)
    }

    @Test
    fun `reglas con fallos degradan la confianza a MEDIUM`() {
        val rules = ruleEvaluation(RuleVerdict.FAIL)

        val result = engine.assess(offer(), metrics(), rules)

        assertEquals(ConfidenceLevel.MEDIUM, result.level)
        assertTrue(result.isActionable)
        assertTrue(result.percent < 80)
    }

    @Test
    fun `reglas con advertencias degradan la confianza a MEDIUM`() {
        val rules = ruleEvaluation(RuleVerdict.WARNING)

        val result = engine.assess(offer(), metrics(), rules)

        assertEquals(ConfidenceLevel.MEDIUM, result.level)
    }

    @Test
    fun `reglas limpias suben ligeramente la confianza`() {
        val base = engine.assess(offer(), metrics())
        val boosted = engine.assess(offer(), metrics(), ruleEvaluation(RuleVerdict.PASS))

        assertTrue(boosted.percent >= base.percent)
        assertEquals(ConfidenceLevel.HIGH, boosted.level)
    }

    @Test
    fun `moneda faltante reduce la confianza pero sigue siendo accionable`() {
        val noCurrency =
            TripOffer(
                platform = RidePlatform.UBER,
                timestampMillis = 1_700_000_000_000,
                estimatedTotal = 100.0,
                distanceKm = 10.0,
                durationMin = 30.0,
                currency = null,
            )

        val result = engine.assess(noCurrency, metrics())

        assertEquals(ConfidenceLevel.HIGH, result.level)
        assertTrue(result.percent < 80)
        assertTrue(result.reasons.any { it.contains("Moneda") })
    }

    @Test
    fun `el porcentaje nunca sale del rango 0 a 100`() {
        val results =
            listOf(
                engine.assess(offer(), metrics()),
                engine.assess(offer(), metrics(), ruleEvaluation(RuleVerdict.FAIL)),
                engine.assess(offer(), metrics(), ruleEvaluation(RuleVerdict.PASS)),
            )

        results.forEach { assertTrue("${it.percent} fuera de rango", it.percent in 0..100) }
    }

    private fun offer(): TripOffer =
        TripOffer(
            platform = RidePlatform.UBER,
            timestampMillis = 1_700_000_000_000,
            estimatedTotal = 100.0,
            distanceKm = 10.0,
            durationMin = 30.0,
            currency = "MXN",
        )

    private fun metrics(): ProfitMetrics =
        ProfitMetrics(
            estimatedTotal = 100.0,
            distanceKm = 10.0,
            durationMin = 30.0,
            totalCost = 40.0,
            estimatedProfit = 60.0,
            profitPerKm = 6.0,
            profitPerHour = 120.0,
            marginPercent = 60.0,
        )

    private fun ruleEvaluation(verdict: RuleVerdict): RuleEvaluation =
        RuleEvaluation(
            results =
                listOf(
                    RuleResult(
                        ruleName = "Test",
                        verdict = verdict,
                        message = "test",
                    ),
                ),
        )
}
