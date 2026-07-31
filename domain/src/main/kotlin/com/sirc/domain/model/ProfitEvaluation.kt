package com.sirc.domain.model

/** Evaluación completa de una oferta: métricas + decisión + razones legibles. */
data class ProfitEvaluation(
    val offer: TripOffer,
    val metrics: ProfitMetrics,
    val decision: Decision,
    val reasons: List<String>,
)
