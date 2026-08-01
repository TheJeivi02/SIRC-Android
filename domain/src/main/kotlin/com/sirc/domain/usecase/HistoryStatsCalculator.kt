package com.sirc.domain.usecase

import com.sirc.domain.model.DayStat
import com.sirc.domain.model.Decision
import com.sirc.domain.model.HistoryStats
import com.sirc.domain.model.OfferHistoryEntry
import java.util.Calendar

/** Calcula el [HistoryStats] del Dashboard a partir del historial persistido. */
object HistoryStatsCalculator {
    fun calculate(entries: List<OfferHistoryEntry>): HistoryStats {
        if (entries.isEmpty()) return HistoryStats()

        val accepted = entries.count { it.decision == Decision.PROFITABLE }
        val rejected = entries.count { it.decision == Decision.NOT_PROFITABLE }
        val marginal = entries.count { it.decision == Decision.MARGINAL }

        val totalProfit = entries.map { it.estimatedProfit }.sum()
        val acceptance =
            if (accepted + rejected > 0) {
                accepted * 100.0 / (accepted + rejected)
            } else {
                0.0
            }

        val perKm =
            entries
                .filter { it.distanceKm != null && it.distanceKm!! > 0 && it.estimatedProfit > 0 }
                .map { it.estimatedProfit / it.distanceKm!! }
        val avgPerKm = perKm.averageOrZero()

        val perHour =
            entries
                .filter { it.durationMin != null && it.durationMin!! > 0 && it.estimatedProfit > 0 }
                .map { it.estimatedProfit / (it.durationMin!! / MINUTES_PER_HOUR) }
        val avgPerHour = perHour.averageOrZero()

        val processing =
            entries.mapNotNull { it.processingMillis }.filter { it > 0 }
        val avgProcessing = processing.averageOrZero()

        val confidence = entries.mapNotNull { it.confidencePercent?.toDouble() }.filter { it in 0.0..100.0 }
        val avgConfidence = confidence.averageOrZero()

        return HistoryStats(
            offersAnalyzed = entries.size,
            accepted = accepted,
            rejected = rejected,
            marginal = marginal,
            acceptancePercent = acceptance,
            totalEstimatedProfit = totalProfit,
            avgProfitPerKm = avgPerKm,
            avgProfitPerHour = avgPerHour,
            avgProcessingMillis = avgProcessing,
            avgConfidencePercent = avgConfidence,
            daily = groupByDay(entries),
        )
    }

    private fun groupByDay(entries: List<OfferHistoryEntry>): List<DayStat> {
        val byDay = LinkedHashMap<Long, MutableList<OfferHistoryEntry>>()
        entries.sortedBy { it.timestampMillis }.forEach { entry ->
            byDay.getOrPut(startOfDay(entry.timestampMillis)) { mutableListOf() }.add(entry)
        }
        return byDay.map { (dayStart, dayEntries) ->
            DayStat(
                dayStartMillis = dayStart,
                offers = dayEntries.size,
                profit = dayEntries.map { it.estimatedProfit }.sum(),
            )
        }
    }

    private fun startOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else sum() / size

    private const val MINUTES_PER_HOUR = 60.0
}
