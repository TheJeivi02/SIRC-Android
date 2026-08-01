package com.sirc.domain.model

/** Estadísticas agregadas por día (para el gráfico del Dashboard). */
data class DayStat(
    val dayStartMillis: Long,
    val offers: Int,
    val profit: Double,
)

/**
 * Estadísticas del Dashboard de la sesión/historial.
 *
 * Todas las métricas se derivan de las [OfferHistoryEntry] persistidas, por lo
 * que el dashboard es 100 % reproducible desde Room.
 */
data class HistoryStats(
    val offersAnalyzed: Int = 0,
    val accepted: Int = 0,
    val rejected: Int = 0,
    val marginal: Int = 0,
    val acceptancePercent: Double = 0.0,
    val totalEstimatedProfit: Double = 0.0,
    val avgProfitPerKm: Double = 0.0,
    val avgProfitPerHour: Double = 0.0,
    val avgProcessingMillis: Double = 0.0,
    val avgConfidencePercent: Double = 0.0,
    val daily: List<DayStat> = emptyList(),
)
