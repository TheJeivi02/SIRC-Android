package com.sirc.domain.engine

import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferRecommendation
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.Recommendation
import javax.inject.Inject

/**
 * Motor de Recomendación: traduce una [ProfitEvaluation] en una recomendación
 * accionable para el conductor.
 *
 * Función pura y desacoplada de la UI: el overlay solo renderiza el resultado.
 */
class RecommendationEngine @Inject constructor() {
    fun recommend(evaluation: ProfitEvaluation): OfferRecommendation =
        when (evaluation.decision) {
            Decision.PROFITABLE -> accept(evaluation)
            Decision.NOT_PROFITABLE -> reject(evaluation)
            Decision.MARGINAL -> warning(evaluation)
        }

    private fun accept(evaluation: ProfitEvaluation): OfferRecommendation {
        val margin = evaluation.metrics.marginPercent
        return OfferRecommendation(
            recommendation = Recommendation.ACCEPT,
            mainReason = "El viaje cumple tu objetivo de rentabilidad",
            metricsUsed = listOf("Ganancia/km", "Ganancia/hora", "Margen"),
            confidencePercent = confidence(margin),
        )
    }

    private fun reject(evaluation: ProfitEvaluation): OfferRecommendation {
        val margin = evaluation.metrics.marginPercent
        return OfferRecommendation(
            recommendation = Recommendation.REJECT,
            mainReason = "El viaje no cubre los costos (pierdes dinero)",
            metricsUsed = listOf("Beneficio neto", "Margen"),
            confidencePercent = confidence(margin),
        )
    }

    private fun warning(evaluation: ProfitEvaluation): OfferRecommendation {
        return OfferRecommendation(
            recommendation = Recommendation.WARNING,
            mainReason = evaluation.reasons.firstOrNull() ?: "El viaje está al límite de los umbrales",
            metricsUsed = listOf("Ganancia/km", "Ganancia/hora"),
            confidencePercent = 50,
        )
    }

    /**
     * Confianza 0-100: a mayor magnitud del margen (positivo o negativo), más
     * clara la señal. Los casos límite quedan en 50.
     */
    private fun confidence(marginPercent: Double): Int =
        (50 + (kotlin.math.abs(marginPercent) / 3.0).toInt()).coerceIn(50, 98)
}
