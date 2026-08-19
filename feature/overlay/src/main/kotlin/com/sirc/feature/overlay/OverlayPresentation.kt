package com.sirc.feature.overlay

import com.sirc.core.ui.theme.ProfitState
import com.sirc.core.ui.theme.recommendationLabel
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.GoalStatus
import com.sirc.domain.model.ProfitMetrics

/**
 * Modelo de presentación del overlay con jerarquía de 3 niveles:
 * 1. decisión (semáforo dominante), 2. oferta, 3. métricas (cada una con su
 * propio semáforo según su objetivo).
 *
 * Sin texto explicativo: el conductor reconoce de un vistazo por color si cada
 * dato cumple su objetivo. Es lógica de presentación pura (JVM testable): no
 * contiene tipos Compose renderizables; la capa UI mapea el [MetricTone] y el
 * [ProfitState] a colores.
 */
data class OverlayPresentation(
    val decision: DecisionPresentation?,
    val offer: OfferPresentation?,
    val metricRows: List<MetricRowPresentation>,
)

/** Nivel 1 — decisión: etiqueta accionable + estado semáforo. */
data class DecisionPresentation(
    val label: String,
    val state: ProfitState,
)

/** Nivel 2 — oferta: plataforma, monto y resumen derivado (distancia · duración). */
data class OfferPresentation(
    val platform: String,
    val amount: String,
    val summary: String?,
)

/** Nivel 3 — fila de métricas de dos columnas (la segunda opcional). */
data class MetricRowPresentation(
    val left: MetricCellPresentation,
    val right: MetricCellPresentation?,
)

/** Celda de métrica lista para mostrar: etiqueta + valor formateado + tono. */
data class MetricCellPresentation(
    val label: String,
    val value: String,
    val tone: MetricTone,
)

/** Tono semáforo de un valor. La UI lo mapea a color del tema. */
enum class MetricTone {
    /** Valor neutro (blanco principal). */
    NEUTRAL,

    /** Rentable (verde). */
    POSITIVE,

    /** Cerca del objetivo pero sin cumplirlo (naranja). */
    WARNING,

    /** No rentable (rojo). */
    NEGATIVE,

    /** Dato de apoyo, poco prominente (blanco atenuado). */
    MUTED,
}

/**
 * Construye el [OverlayPresentation] del overlay a partir del estado real.
 *
 * Devuelve null sin evaluación (la UI muestra entonces el estado del pipeline).
 * NO inventa datos: solo reformatea lo que entregan los motores/pipeline.
 */
fun mapToOverlayPresentation(
    state: OverlayUiState,
    engine: ProfitEngine,
): OverlayPresentation? {
    val evaluation = state.evaluation ?: return null
    val config = state.config
    val metrics = evaluation.metrics
    val currency = evaluation.offer.currency

    val decision =
        if (config.showDecision) {
            val recommendation = state.recommendation
            if (recommendation != null) {
                DecisionPresentation(
                    label = recommendationLabel(recommendation.recommendation),
                    state = ProfitState.fromRecommendation(recommendation.recommendation),
                )
            } else {
                val profitState = ProfitState.fromDecision(evaluation.decision)
                DecisionPresentation(label = profitState.label, state = profitState)
            }
        } else {
            null
        }

    val offer =
        OfferPresentation(
            platform = evaluation.offer.platform.displayName,
            amount = engine.formatCurrency(metrics.estimatedTotal, currency),
            summary = buildSummary(metrics, config.showTripSummary, engine),
        )

    val profitPerHour = metrics.profitPerHour
    val profitPerKm = metrics.profitPerKm
    val cells =
        listOfNotNull(
            if (config.showProfit && metrics.hasDistance) {
                MetricCellPresentation(
                    "GANANCIA",
                    engine.formatCurrency(metrics.estimatedProfit, currency),
                    goalTone(metrics.netGoal),
                )
            } else {
                null
            },
            if (config.showProfitPerHour && profitPerHour != null) {
                MetricCellPresentation(
                    "POR HORA",
                    "${engine.formatCurrency(profitPerHour, currency)}/h",
                    goalTone(metrics.profitPerHourGoal),
                )
            } else {
                null
            },
            if (config.showProfitPerKm && profitPerKm != null) {
                MetricCellPresentation(
                    "POR KM",
                    "${engine.formatCurrency(profitPerKm, currency)}/km",
                    goalTone(metrics.profitPerKmGoal),
                )
            } else {
                null
            },
            if (config.showTripSummary && metrics.hasDistance) {
                MetricCellPresentation(
                    "COSTO EST.",
                    engine.formatCurrency(metrics.totalCost, currency),
                    MetricTone.MUTED,
                )
            } else {
                null
            },
        )
    val metricRows = cells.chunked(2).map { MetricRowPresentation(left = it[0], right = it.getOrNull(1)) }

    return OverlayPresentation(
        decision = decision,
        offer = offer,
        metricRows = metricRows,
    )
}

private fun buildSummary(
    metrics: ProfitMetrics,
    showTripSummary: Boolean,
    engine: ProfitEngine,
): String? {
    if (!showTripSummary) return null
    val distance = if (metrics.distanceKm > 0) "${metrics.distanceKm} km" else null
    val duration = if (metrics.durationMin > 0) engine.formatHours(metrics.durationMin) else null
    return listOfNotNull(distance, duration).joinToString(" · ").ifBlank { null }
}

/**
 * Tono semáforo de una métrica según su objetivo: verde si cumple, naranja si
 * está cerca, rojo si no cumple. Cada dato del overlay refleja su propio
 * semáforo, independiente de la decisión general.
 */
private fun goalTone(status: GoalStatus?): MetricTone =
    when (status) {
        GoalStatus.MET -> MetricTone.POSITIVE
        GoalStatus.NEAR -> MetricTone.WARNING
        GoalStatus.FAILED -> MetricTone.NEGATIVE
        null -> MetricTone.NEUTRAL
    }
