package com.sirc.domain.engine

import com.sirc.domain.model.Decision
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private val engine = RecommendationEngine()

    @Test
    fun `decisión rentable produce ACCEPT con confianza alta`() {
        val recommendation = engine.recommend(evaluation(Decision.PROFITABLE, margin = 60.0))

        assertEquals(Recommendation.ACCEPT, recommendation.recommendation)
        assertTrue(recommendation.confidencePercent in 50..98)
        assertEquals("El viaje supera los umbrales de rentabilidad", recommendation.mainReason)
        assertTrue(recommendation.metricsUsed.isNotEmpty())
    }

    @Test
    fun `decisión no rentable produce REJECT`() {
        val recommendation = engine.recommend(evaluation(Decision.NOT_PROFITABLE, margin = -80.0))

        assertEquals(Recommendation.REJECT, recommendation.recommendation)
        assertEquals("El viaje no cubre los costos estimados", recommendation.mainReason)
    }

    @Test
    fun `decisión marginal produce WARNING con motivo del motor`() {
        val recommendation = engine.recommend(evaluation(Decision.MARGINAL, margin = 10.0))

        assertEquals(Recommendation.WARNING, recommendation.recommendation)
        assertEquals(50, recommendation.confidencePercent)
        assertTrue(recommendation.mainReason.isNotBlank())
    }

    @Test
    fun `la confianza nunca sale del rango 0 a 100`() {
        val extremos =
            listOf(
                evaluation(Decision.PROFITABLE, margin = 10_000.0),
                evaluation(Decision.NOT_PROFITABLE, margin = -10_000.0),
                evaluation(Decision.PROFITABLE, margin = 0.1),
            )

        extremos.forEach { evaluation ->
            val confidence = engine.recommend(evaluation).confidencePercent
            assertTrue("confianza $confidence fuera de rango", confidence in 0..100)
        }
    }

    private fun evaluation(
        decision: Decision,
        margin: Double,
    ): ProfitEvaluation {
        val offer =
            TripOffer(
                platform = RidePlatform.UBER,
                timestampMillis = 1_700_000_000_000,
                estimatedTotal = 100.0,
                distanceKm = 10.0,
                durationMin = 30.0,
            )
        val metrics =
            ProfitMetrics(
                estimatedTotal = 100.0,
                distanceKm = 10.0,
                durationMin = 30.0,
                totalCost = 50.0,
                estimatedProfit = if (decision == Decision.NOT_PROFITABLE) -20.0 else 50.0,
                profitPerKm = 5.0,
                profitPerHour = 100.0,
                marginPercent = margin,
            )
        val reasons =
            if (decision == Decision.MARGINAL) {
                listOf("Ganancia/hora menor al mínimo configurado")
            } else {
                emptyList()
            }
        return ProfitEvaluation(
            offer = offer,
            metrics = metrics,
            decision = decision,
            reasons = reasons,
        )
    }
}
