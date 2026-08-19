package com.sirc.domain.model

/**
 * Métricas de rentabilidad derivadas de la oferta.
 *
 * [profitPerKm] es null cuando la distancia es desconocida (no se fabrica una
 * cifra falsa). [profitPerHour] es null cuando la distancia o la duración son
 * desconocidas (la ganancia real depende de ambos datos). [totalCost] solo
 * incluye costos con datos disponibles: sin distancia, solo el costo fijo.
 */
data class ProfitMetrics(
    val estimatedTotal: Double,
    val distanceKm: Double,
    val durationMin: Double,
    val totalCost: Double,
    val estimatedProfit: Double,
    val profitPerKm: Double?,
    val profitPerHour: Double?,
    val marginPercent: Double,
) {
    /** La distancia es confiable solo si es mayor que cero (0 = desconocida). */
    val hasDistance: Boolean
        get() = distanceKm > 0.0

    /** La duración es confiable solo si es mayor que cero (0 = desconocida). */
    val hasDuration: Boolean
        get() = durationMin > 0.0
}
