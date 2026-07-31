package com.sirc.domain.model

/**
 * Umbrales de decisión del conductor.
 *
 * Si no se cumplen los umbrales mínimos, la oferta se considera no rentable.
 */
data class DecisionThresholds(
    val minProfit: Double,
    val minProfitPerHour: Double,
)
