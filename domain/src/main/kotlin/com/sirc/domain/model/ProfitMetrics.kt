package com.sirc.domain.model

/**
 * Estado de una métrica frente a su objetivo: alimenta el semáforo por dato
 * del overlay (verde = cumple, naranja = cerca, rojo = no cumple).
 */
enum class GoalStatus {
    /** Cumple el objetivo (verde). */
    MET,

    /** Cerca del objetivo pero sin cumplirlo (naranja). */
    NEAR,

    /** No cumple: sin ganancia o pérdida (rojo). */
    FAILED,
}

/**
 * Métricas de rentabilidad derivadas de la oferta.
 *
 * [profitPerKm] es null cuando la distancia es desconocida (no se fabrica una
 * cifra falsa). [profitPerHour] es null SOLO cuando la duración es
 * desconocida (se calcula con el monto y la duración disponibles). [totalCost]
 * solo incluye costos con datos disponibles: sin distancia, solo el costo fijo.
 *
 * Cada métrica derivada lleva su [GoalStatus]: verde si cumple su objetivo,
 * naranja si está cerca (positiva pero bajo el objetivo) y rojo si no cumple.
 * El estado es null cuando la métrica no existe (dimensión desconocida).
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
    /** Ganancia neta frente a cero: MET > 0, NEAR == 0 (break-even), FAILED < 0. */
    val netGoal: GoalStatus =
        if (estimatedProfit > 0.0) {
            GoalStatus.MET
        } else if (estimatedProfit == 0.0) {
            GoalStatus.NEAR
        } else {
            GoalStatus.FAILED
        },
    /** Estado de [profitPerKm] frente a su objetivo; null sin distancia. */
    val profitPerKmGoal: GoalStatus? = null,
    /** Estado de [profitPerHour] frente a su objetivo; null sin duración. */
    val profitPerHourGoal: GoalStatus? = null,
) {
    /** La distancia es confiable solo si es mayor que cero (0 = desconocida). */
    val hasDistance: Boolean
        get() = distanceKm > 0.0

    /** La duración es confiable solo si es mayor que cero (0 = desconocida). */
    val hasDuration: Boolean
        get() = durationMin > 0.0
}
