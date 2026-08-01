package com.sirc.domain.model

/**
 * Contexto de evaluación de reglas: la oferta original, sus métricas derivadas
 * y los umbrales configurados por el conductor.
 */
data class RuleContext(
    val offer: TripOffer,
    val metrics: ProfitMetrics,
    val thresholds: RuleThresholds,
)
