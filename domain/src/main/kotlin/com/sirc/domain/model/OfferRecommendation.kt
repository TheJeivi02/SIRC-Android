package com.sirc.domain.model

/**
 * Recomendación generada para una oferta evaluada.
 *
 * Incluye la [recommendation] accionable, el [mainReason] legible para el
 * conductor, las [metricsUsed] que la sustentan y una [confidencePercent]
 * (0-100) que indica qué tan clara es la señal.
 */
data class OfferRecommendation(
    val recommendation: Recommendation,
    val mainReason: String,
    val metricsUsed: List<String>,
    val confidencePercent: Int,
)
