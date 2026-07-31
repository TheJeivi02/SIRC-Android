package com.sirc.domain.model

/** Entrada del historial básico de ofertas evaluadas. */
data class OfferHistoryEntry(
    val id: Long = 0L,
    val platform: RidePlatform,
    val timestampMillis: Long,
    val estimatedTotal: Double?,
    val distanceKm: Double?,
    val durationMin: Double?,
    val estimatedProfit: Double,
    val decision: Decision,
    val summary: String,
)
