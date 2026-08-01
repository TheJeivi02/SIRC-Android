package com.sirc.domain.model

/**
 * Configuración del Overlay.
 *
 * Máximo cuatro indicadores visibles, seleccionables por el usuario.
 * El diseño prioriza velocidad, legibilidad y mínimo consumo.
 */
data class OverlayConfig(
    val showDecision: Boolean = true,
    val showProfit: Boolean = true,
    val showProfitPerHour: Boolean = true,
    val showProfitPerKm: Boolean = false,
    val showTripSummary: Boolean = true,
    val compactMode: Boolean = false,
    val opacityPercent: Int = 95,
    val ttlSeconds: Long = 45,
    val positionXPercent: Float = 50f,
    val positionYPercent: Float = 4f,
    val historyLimit: Int = DEFAULT_HISTORY_LIMIT,
) {
    val activeIndicatorCount: Int
        get() = listOf(showDecision, showProfit, showProfitPerHour, showProfitPerKm, showTripSummary).count { it }

    companion object {
        /** Registros máximos que se conservan en el historial persistente. */
        const val DEFAULT_HISTORY_LIMIT = 500
    }
}
