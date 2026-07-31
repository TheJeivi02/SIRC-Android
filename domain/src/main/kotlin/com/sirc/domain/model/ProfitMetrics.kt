package com.sirc.domain.model

/** Métricas de rentabilidad derivadas de la oferta. */
data class ProfitMetrics(
    val estimatedTotal: Double,
    val distanceKm: Double,
    val durationMin: Double,
    val totalCost: Double,
    val estimatedProfit: Double,
    val profitPerKm: Double,
    val profitPerHour: Double,
    val marginPercent: Double,
)
