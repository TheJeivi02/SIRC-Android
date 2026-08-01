package com.sirc.domain.model

/**
 * Evaluación completa de una oferta: el resultado del motor base
 * ([ProfitEvaluation]) más el desglose de costos derivado de la
 * [DriverConfig].
 */
data class ProfitEvaluationDetailed(
    val evaluation: ProfitEvaluation,
    val breakdown: ProfitBreakdown,
)
