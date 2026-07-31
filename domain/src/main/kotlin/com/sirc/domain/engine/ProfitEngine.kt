package com.sirc.domain.engine

import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.TripOffer
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Motor de Rentabilidad (núcleo del producto).
 *
 * Es una función pura: dada una oferta, los costos del conductor y los umbrales
 * de decisión, produce métricas y una decisión. Sin estado, sin I/O, testeable.
 */
class ProfitEngine @Inject constructor() {
    fun evaluate(
        offer: TripOffer,
        costs: DriverCosts,
        thresholds: DecisionThresholds,
    ): ProfitEvaluation {
        require(offer.hasEnoughData) { "La oferta no contiene datos suficientes para evaluar." }
        val total = offer.estimatedTotal ?: offer.fareAmount ?: 0.0
        val distance = offer.distanceKm ?: 0.0
        val duration = offer.durationMin ?: 0.0

        val costDistance = distance * costs.costPerKm
        val costTime = duration * costs.costPerMinute
        val totalCost = costs.costPerTrip + costDistance + costTime

        val profit = total - totalCost
        val profitPerKm = if (distance > 0.0) profit / distance else profit
        val profitPerHour = if (duration > 0.0) profit / (duration / 60.0) else profit * 4.0
        val marginPercent = if (total > 0.0) profit / total * 100.0 else 0.0

        val metrics =
            ProfitMetrics(
                estimatedTotal = total,
                distanceKm = distance,
                durationMin = duration,
                totalCost = totalCost,
                estimatedProfit = profit,
                profitPerKm = profitPerKm,
                profitPerHour = profitPerHour,
                marginPercent = marginPercent,
            )

        return ProfitEvaluation(
            offer = offer,
            metrics = metrics,
            decision = decide(metrics, thresholds),
            reasons = buildReasons(metrics, thresholds),
        )
    }

    private fun decide(
        metrics: ProfitMetrics,
        thresholds: DecisionThresholds,
    ): Decision {
        val meetsProfit = metrics.estimatedProfit >= thresholds.minProfit
        val meetsHourly = metrics.profitPerHour >= thresholds.minProfitPerHour
        return when {
            meetsProfit && meetsHourly -> Decision.PROFITABLE
            metrics.estimatedProfit <= 0.0 -> Decision.NOT_PROFITABLE
            else -> Decision.MARGINAL
        }
    }

    private fun buildReasons(
        metrics: ProfitMetrics,
        thresholds: DecisionThresholds,
    ): List<String> {
        val reasons = mutableListOf<String>()
        if (metrics.estimatedProfit < thresholds.minProfit) {
            reasons.add("Ganancia menor al mínimo configurado")
        }
        if (metrics.profitPerHour < thresholds.minProfitPerHour) {
            reasons.add("Ganancia/hora menor al mínimo configurado")
        }
        if (reasons.isEmpty()) {
            reasons.add("Supera los umbrales configurados")
        }
        return reasons
    }

    fun formatCurrency(
        amount: Double,
        currency: String?,
    ): String {
        val code = currency ?: "USD"
        val symbol = CURRENCY_SYMBOLS[code] ?: "$"
        val rounded = (amount * 100).roundToInt() / 100.0
        val text =
            if (rounded == rounded.toLong().toDouble()) {
                rounded.toLong().toString()
            } else {
                rounded.toString()
            }
        return "$symbol$text"
    }

    fun formatHours(durationMin: Double): String {
        val minutes = durationMin.roundToInt()
        if (minutes < 60) return "$minutes min"
        val hours = minutes / 60
        val rem = minutes % 60
        return if (rem == 0) "${hours}h" else "${hours}h ${rem}m"
    }

    companion object {
        private val CURRENCY_SYMBOLS =
            mapOf(
                "USD" to "$",
                "MXN" to "$",
                "COP" to "\$",
                "BRL" to "R\$",
                "PEN" to "S/",
                "ARS" to "\$",
                "EUR" to "€",
            )
    }
}
