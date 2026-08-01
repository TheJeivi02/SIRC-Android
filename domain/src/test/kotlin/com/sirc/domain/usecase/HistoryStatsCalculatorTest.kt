package com.sirc.domain.usecase

import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryStatsCalculatorTest {
    @Test
    fun `estadisticas vacias cuando no hay entradas`() {
        val stats = HistoryStatsCalculator.calculate(emptyList())

        assertEquals(0, stats.offersAnalyzed)
        assertEquals(0.0, stats.acceptancePercent, 0.001)
    }

    @Test
    fun `calcula porcentaje de aceptacion y conteos`() {
        val stats =
            HistoryStatsCalculator.calculate(
                listOf(
                    entry(decision = Decision.PROFITABLE),
                    entry(decision = Decision.PROFITABLE),
                    entry(decision = Decision.NOT_PROFITABLE),
                    entry(decision = Decision.MARGINAL),
                ),
            )

        assertEquals(4, stats.offersAnalyzed)
        assertEquals(2, stats.accepted)
        assertEquals(1, stats.rejected)
        assertEquals(1, stats.marginal)
        assertEquals(66.666, stats.acceptancePercent, 0.001)
    }

    @Test
    fun `promedia ganancia por hora y por km`() {
        val stats =
            HistoryStatsCalculator.calculate(
                listOf(
                    entry(profit = 100.0, distanceKm = 10.0, durationMin = 60.0),
                    entry(profit = 50.0, distanceKm = 10.0, durationMin = 60.0),
                ),
            )

        assertEquals(7.5, stats.avgProfitPerKm, 0.001)
        assertEquals(75.0, stats.avgProfitPerHour, 0.001)
        assertEquals(150.0, stats.totalEstimatedProfit, 0.001)
    }

    @Test
    fun `agrupa por dia y suma la ganancia diaria`() {
        val dayStart = startOfToday()
        val stats =
            HistoryStatsCalculator.calculate(
                listOf(
                    entry(profit = 100.0, timestampMillis = dayStart),
                    entry(profit = 50.0, timestampMillis = dayStart + 60_000),
                ),
            )

        assertEquals(1, stats.daily.size)
        assertEquals(2, stats.daily[0].offers)
        assertEquals(150.0, stats.daily[0].profit, 0.001)
    }

    @Test
    fun `promedia confianza cuando esta presente`() {
        val stats =
            HistoryStatsCalculator.calculate(
                listOf(
                    entry(confidencePercent = 80),
                    entry(confidencePercent = 90),
                ),
            )

        assertEquals(85.0, stats.avgConfidencePercent, 0.001)
        assertTrue(stats.acceptancePercent in 0.0..100.0)
    }

    private fun entry(
        decision: Decision = Decision.PROFITABLE,
        profit: Double = 100.0,
        distanceKm: Double = 8.0,
        durationMin: Double = 20.0,
        timestampMillis: Long = System.currentTimeMillis(),
        confidencePercent: Int? = null,
    ): OfferHistoryEntry =
        OfferHistoryEntry(
            platform = RidePlatform.UBER,
            timestampMillis = timestampMillis,
            estimatedTotal = 125.0,
            distanceKm = distanceKm,
            durationMin = durationMin,
            estimatedProfit = profit,
            decision = decision,
            summary = "Viaje aceptado",
            confidencePercent = confidencePercent,
            confidenceLevel = confidencePercent?.let { if (it >= 70) "HIGH" else "MEDIUM" },
            recommendation = Recommendation.ACCEPT,
            processingMillis = 12.5,
        )

    private fun startOfToday(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
