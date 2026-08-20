package com.sirc.domain.engine

import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.GoalStatus
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.TripOffer
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Motor de Rentabilidad (núcleo del producto).
 *
 * Es una función pura: dada una oferta, los costos del conductor y los
 * objetivos de decisión, produce métricas y una decisión. Sin estado, sin I/O,
 * testeable.
 *
 * Modelo económico aprobado (WP-12-CALC-03):
 * - El ingreso es el monto real de la oferta (no se modifica).
 * - El costo real es `costPerTrip + distanceKm × costPerKm`; el costo por
 *   minuto no participa y el objetivo de ganancia NO es un costo.
 * - Distancia/duración `0` se interpretan como desconocidas: no se fabrican
 *   métricas derivadas falsas. Cada dimensión disponible se calcula por
 *   separado: $/hora solo necesita la duración, $/km solo la distancia.
 * - Decisión: pérdida real (ganancia < 0) → REJECT; ACCEPT solo con ambas
 *   dimensiones y ambos objetivos cumplidos; en cualquier otro caso → MARGINAL.
 */
class ProfitEngine @Inject constructor() {
    fun evaluate(
        offer: TripOffer,
        costs: DriverCosts,
        thresholds: DecisionThresholds,
    ): ProfitEvaluation {
        require(offer.hasEnoughData) { "La oferta no contiene el monto para evaluar." }
        val total = offer.estimatedTotal ?: offer.fareAmount ?: 0.0
        val distance = offer.distanceKm
        val duration = offer.durationMin ?: 0.0
        val hasDistance = distance != null && distance > 0.0
        val hasDuration = duration > 0.0

        // El costo por distancia solo se cuenta con distancia confiable; sin
        // ella, la ganancia es el mejor caso (solo costo fijo) y se marca la
        // falta de distancia en lugar de presentarla como real.
        val costDistance = if (hasDistance) distance!! * costs.costPerKm else 0.0
        val totalCost = costs.costPerTrip + costDistance

        val profit = total - totalCost
        val profitPerKm = if (hasDistance) profit / distance!! else null
        val profitPerHour = if (hasDuration) profit / (duration / 60.0) else null
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
                profitPerKmGoal = if (hasDistance) goalOf(profitPerKm!!, thresholds.minProfitPerKm) else null,
                profitPerHourGoal = if (hasDuration) goalOf(profitPerHour!!, thresholds.minProfitPerHour) else null,
            )

        val decision = decide(metrics, thresholds)
        return ProfitEvaluation(
            offer = offer,
            metrics = metrics,
            decision = decision,
            reasons = buildReasons(metrics, decision, thresholds),
        )
    }

    /**
     * Jerarquía aprobada: REJECT solo con pérdida real; ACCEPT solo cuando la
     * ganancia es real (distancia y duración conocidas) y se cumplen los dos
     * objetivos; el resto es MARGINAL.
     */
    private fun decide(
        metrics: ProfitMetrics,
        thresholds: DecisionThresholds,
    ): Decision {
        if (metrics.estimatedProfit < 0.0) return Decision.NOT_PROFITABLE
        val profitPerKm = metrics.profitPerKm ?: return Decision.MARGINAL
        val profitPerHour = metrics.profitPerHour ?: return Decision.MARGINAL
        val meetsPerKm = profitPerKm >= thresholds.minProfitPerKm
        val meetsHourly = profitPerHour >= thresholds.minProfitPerHour
        return if (meetsPerKm && meetsHourly) Decision.PROFITABLE else Decision.MARGINAL
    }

    private fun buildReasons(
        metrics: ProfitMetrics,
        decision: Decision,
        thresholds: DecisionThresholds,
    ): List<String> =
        when (decision) {
            Decision.NOT_PROFITABLE -> listOf("El viaje no cubre los costos estimados (pierdes dinero)")

            Decision.PROFITABLE -> listOf("Cumple tus objetivos de rentabilidad")

            Decision.MARGINAL ->
                when {
                    !metrics.hasDistance && !metrics.hasDuration ->
                        listOf("Faltan la distancia y la duración para evaluar la rentabilidad")

                    !metrics.hasDistance -> {
                        val hourly =
                            if ((metrics.profitPerHour ?: 0.0) >= thresholds.minProfitPerHour) {
                                "Cumple el objetivo por hora"
                            } else {
                                "Ganancia/hora menor al objetivo"
                            }
                        listOf(hourly, "Falta la distancia para confirmar el objetivo completo")
                    }

                    !metrics.hasDuration -> {
                        val perKm =
                            if ((metrics.profitPerKm ?: 0.0) >= thresholds.minProfitPerKm) {
                                "Cumple el objetivo por km"
                            } else {
                                "Ganancia/km menor al objetivo"
                            }
                        listOf(perKm, "Falta la duración para confirmar el objetivo completo")
                    }

                    metrics.estimatedProfit == 0.0 -> listOf("El viaje solo cubre los costos (sin ganancia)")

                    else -> {
                        val reasons = mutableListOf<String>()
                        if ((metrics.profitPerHour ?: 0.0) < thresholds.minProfitPerHour) {
                            reasons.add("Ganancia/hora menor al objetivo")
                        }
                        if ((metrics.profitPerKm ?: 0.0) < thresholds.minProfitPerKm) {
                            reasons.add("Ganancia/km menor al objetivo")
                        }
                        reasons.ifEmpty { listOf("El viaje está al límite de los objetivos") }
                    }
                }
        }

    /**
     * Semáforo de una métrica frente a su objetivo: verde si cumple, naranja
     * si es positiva pero no llega al objetivo, rojo si no genera ganancia.
     */
    private fun goalOf(
        value: Double,
        min: Double,
    ): GoalStatus =
        when {
            value >= min -> GoalStatus.MET
            value > 0.0 -> GoalStatus.NEAR
            else -> GoalStatus.FAILED
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
