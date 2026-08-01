package com.sirc.core.platform

/**
 * Variante de oferta detectada por el [OfferParserOrchestrator].
 *
 * Permite que el pipeline y el panel de depuración sepan qué estrategia de
 * parsing se usó y qué tipo de viaje es (radar, reserva, moto, XL, etc.).
 */
enum class OfferType {
    /** Solicitud estándar (Uber X, Comfort, etc.). */
    UBER_REQUEST,

    /** Oferta de radar (explorar el mapa por viajes cercanos). */
    UBER_RADAR,

    /** Viaje reservado/programado. */
    UBER_RESERVATION,

    /** Viaje en Uber Moto (2 ruedas). */
    UBER_MOTO,

    /** Viaje en Uber XL (vehículos grandes). */
    UBER_XL,

    /** No se pudo determinar la variante: se usa el extractor genérico. */
    GENERIC,
}
