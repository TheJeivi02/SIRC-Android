package com.sirc.domain.model

/**
 * Umbrales de decisión del conductor.
 *
 * Son los dos indicadores principales del MVP: ganancia mínima por kilómetro y
 * ganancia mínima por hora. Si no se cumplen, la oferta se considera no
 * rentable.
 */
data class DecisionThresholds(
    val minProfitPerKm: Double,
    val minProfitPerHour: Double,
) {
    companion object {
        fun default(): DecisionThresholds = DecisionThresholds(minProfitPerKm = 4.0, minProfitPerHour = 120.0)
    }
}
