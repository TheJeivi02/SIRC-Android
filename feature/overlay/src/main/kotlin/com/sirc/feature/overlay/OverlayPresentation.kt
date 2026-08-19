package com.sirc.feature.overlay

import com.sirc.core.ui.theme.ProfitState
import com.sirc.core.ui.theme.recommendationLabel
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.ProfitMetrics

/**
 * Modelo de presentación del overlay con jerarquía de 4 niveles:
 * 1. decisión (dominante), 2. oferta, 3. métricas, 4. secundaria.
 *
 * Es lógica de presentación pura (JVM testable): no contiene tipos Compose
 * renderizables; la capa UI mapea el [MetricTone] y el [ProfitState] a colores.
 */
data class OverlayPresentation(
    val decision: DecisionPresentation?,
    val offer: OfferPresentation?,
    val metricRows: List<MetricRowPresentation>,
    val secondaryLine: String?,
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

    val tone = toneFor(decision)
    val profitPerHour = metrics.profitPerHour
    val profitPerKm = metrics.profitPerKm
    val cells =
        listOfNotNull(
            if (config.showProfit && metrics.hasDistance) {
                MetricCellPresentation(
                    "GANANCIA",
                    engine.formatCurrency(metrics.estimatedProfit, currency),
                    tone,
                )
            } else {
                null
            },
            if (config.showProfitPerHour && profitPerHour != null) {
                MetricCellPresentation(
                    "POR HORA",
                    "${engine.formatCurrency(profitPerHour, currency)}/h",
                    tone,
                )
            } else {
                null
            },
            if (config.showProfitPerKm && profitPerKm != null) {
                MetricCellPresentation(
                    "POR KM",
                    "${engine.formatCurrency(profitPerKm, currency)}/km",
                    tone,
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
        secondaryLine = buildSecondaryLine(state),
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

private fun buildSecondaryLine(state: OverlayUiState): String? {
    val recommendation = state.recommendation
    if (recommendation != null) {
        return "${recommendation.mainReason} · ${recommendation.confidencePercent}% confianza"
    }
    val confidence = state.confidence ?: return null
    return if (confidence.isActionable) {
        val type = state.offerType ?: "Oferta"
        "$type · Confianza ${confidence.percent}% (${confidence.level.name})"
    } else {
        "Información insuficiente · ${confidence.percent}% confianza"
    }
}

/**
 * Tono semáforo de las métricas según el estado de la decisión: ACCEPT verde,
 * WARNING neutro, REJECT rojo. No usa el objetivo ni heurísticas de margen.
 */
private fun toneFor(decision: DecisionPresentation?): MetricTone =
    when (decision?.state) {
        ProfitState.PROFITABLE -> MetricTone.POSITIVE
        ProfitState.NOT_PROFITABLE -> MetricTone.NEGATIVE
        else -> MetricTone.NEUTRAL
    }
