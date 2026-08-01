package com.sirc.domain.model

/**
 * Oferta de viaje capturada desde la interfaz visible de una plataforma.
 *
 * No repite la información que el conductor ya ve: solo contiene los datos
 * derivados necesarios para calcular la rentabilidad.
 */
data class TripOffer(
    val platform: RidePlatform,
    val timestampMillis: Long,
    val estimatedTotal: Double? = null,
    val fareAmount: Double? = null,
    val distanceKm: Double? = null,
    val durationMin: Double? = null,
    val currency: String? = null,
    val pickupDistanceKm: Double? = null,
    val rawText: List<String> = emptyList(),
) {
    val hasEnoughData: Boolean
        get() = estimatedTotal != null && ((distanceKm ?: 0.0) > 0.0 || (durationMin ?: 0.0) > 0.0)
}
