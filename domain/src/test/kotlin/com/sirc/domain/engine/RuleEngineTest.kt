package com.sirc.domain.engine

import com.sirc.domain.model.OfferRule
import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.RuleContext
import com.sirc.domain.model.RuleResult
import com.sirc.domain.model.RuleThresholds
import com.sirc.domain.model.RuleVerdict
import com.sirc.domain.model.TripOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {
    private val thresholds =
        RuleThresholds(
            minProfit = 0.0,
            minProfitPerKm = 4.0,
            minProfitPerHour = 120.0,
            maxDistanceKm = 60.0,
            maxPickupKm = 10.0,
            maxTripTimeMin = 180.0,
        )

    @Test
    fun `viaje rentable pasa todas las reglas`() {
        val evaluation = engine().evaluate(context(profitPerKm = 8.0, profitPerHour = 200.0, profit = 60.0))

        assertTrue(evaluation.allPassed)
        assertEquals(6, evaluation.results.size)
        assertTrue(evaluation.failures.isEmpty())
        assertTrue(evaluation.warnings.isEmpty())
    }

    @Test
    fun `ganancia negativa falla la regla de ganancia minima`() {
        val evaluation = engine().evaluate(context(profit = -10.0))

        assertFalse(evaluation.allPassed)
        assertEquals(1, evaluation.failures.size)
        assertEquals("Ganancia mínima", evaluation.failures.first().ruleName)
        assertEquals(RuleVerdict.FAIL, evaluation.failures.first().verdict)
    }

    @Test
    fun `ganancia por km baja produce advertencia`() {
        val evaluation = engine().evaluate(context(profitPerKm = 4.1, profitPerHour = 200.0, profit = 60.0))

        assertFalse(evaluation.allPassed)
        assertEquals(1, evaluation.warnings.size)
        assertEquals("Ganancia/km mínima", evaluation.warnings.first().ruleName)
    }

    @Test
    fun `distancia excesiva falla la regla de distancia maxima`() {
        val evaluation = engine().evaluate(context(distanceKm = 90.0))

        assertFalse(evaluation.allPassed)
        assertEquals(1, evaluation.failures.size)
        assertEquals("Distancia máxima", evaluation.failures.first().ruleName)
    }

    @Test
    fun `recogida lejana falla la regla de recogida maxima`() {
        val evaluation = engine().evaluate(context(pickupKm = 15.0, distanceKm = 20.0))

        assertFalse(evaluation.allPassed)
        assertEquals(1, evaluation.failures.size)
        assertEquals("Recogida máxima", evaluation.failures.first().ruleName)
    }

    @Test
    fun `duracion excesiva falla la regla de tiempo maximo`() {
        val evaluation = engine().evaluate(context(durationMin = 240.0))

        assertFalse(evaluation.allPassed)
        assertEquals(1, evaluation.failures.size)
        assertEquals("Tiempo de viaje máximo", evaluation.failures.first().ruleName)
    }

    @Test
    fun `reglas sin datos del viaje se consideran cumplidas`() {
        val context =
            RuleContext(
                offer = offer(distanceKm = null, durationMin = null, pickupKm = null),
                metrics = metrics(profitPerKm = 8.0, profitPerHour = 200.0, profit = 60.0),
                thresholds = thresholds,
            )
        val evaluation = engine().evaluate(context)

        assertTrue(evaluation.allPassed)
    }

    @Test
    fun `reglas personalizadas se evaluan en orden`() {
        val fakeRules = listOf<OfferRule>(FakeRule("A"), FakeRule("B"))
        val engine = RuleEngine(fakeRules)

        val evaluation = engine.evaluate(context(profitPerKm = 8.0, profitPerHour = 200.0, profit = 60.0))

        assertEquals(listOf("A", "B"), evaluation.results.map { it.ruleName })
    }

    @Test
    fun `resultado expone accesos a fallos advertencias y por regla`() {
        val engine = RuleEngine(listOf(FakeRule("F", RuleVerdict.FAIL), FakeRule("W", RuleVerdict.WARNING)))
        val evaluation = engine.evaluate(context(profitPerKm = 8.0, profitPerHour = 200.0, profit = 60.0))

        assertEquals("F", evaluation.resultFor("F")?.ruleName)
        assertEquals(1, evaluation.failures.size)
        assertEquals(1, evaluation.warnings.size)
        assertTrue(evaluation.hasFailures)
        assertTrue(evaluation.hasWarnings)
    }

    private class FakeRule(
        override val name: String,
        private val verdict: RuleVerdict = RuleVerdict.PASS,
    ) : OfferRule {
        override fun evaluate(context: RuleContext) =
            RuleResult(
                ruleName = name,
                verdict = verdict,
                message = "test",
            )
    }

    private fun engine() = RuleEngine()

    private fun context(
        profitPerKm: Double = 8.0,
        profitPerHour: Double = 200.0,
        profit: Double = 60.0,
        distanceKm: Double = 10.0,
        durationMin: Double = 30.0,
        pickupKm: Double = 2.0,
    ): RuleContext =
        RuleContext(
            offer = offer(distanceKm, durationMin, pickupKm),
            metrics = metrics(profitPerKm, profitPerHour, profit),
            thresholds = thresholds,
        )

    private fun offer(
        distanceKm: Double?,
        durationMin: Double?,
        pickupKm: Double?,
    ): TripOffer =
        TripOffer(
            platform = RidePlatform.UBER,
            timestampMillis = 1_700_000_000_000,
            estimatedTotal = 100.0,
            distanceKm = distanceKm,
            durationMin = durationMin,
            pickupDistanceKm = pickupKm,
        )

    private fun metrics(
        profitPerKm: Double,
        profitPerHour: Double,
        profit: Double,
    ): ProfitMetrics =
        ProfitMetrics(
            estimatedTotal = 100.0,
            distanceKm = 10.0,
            durationMin = 30.0,
            totalCost = 40.0,
            estimatedProfit = profit,
            profitPerKm = profitPerKm,
            profitPerHour = profitPerHour,
            marginPercent = 50.0,
        )
}
