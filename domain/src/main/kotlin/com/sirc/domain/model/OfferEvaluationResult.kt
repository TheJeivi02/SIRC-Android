package com.sirc.domain.model

/**
 * Resultado agregado del análisis de una oferta: evaluación base con métricas,
 * desglose de costos y recomendación accionable.
 *
 * Es el contrato que consumen el overlay, el historial temporal y el panel de
 * depuración.
 */
data class OfferEvaluationResult(
    val evaluation: ProfitEvaluation,
    val breakdown: ProfitBreakdown,
    val recommendation: OfferRecommendation,
)
