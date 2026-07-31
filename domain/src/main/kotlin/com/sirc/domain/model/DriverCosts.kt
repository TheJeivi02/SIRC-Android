package com.sirc.domain.model

/** Configuración de costos del conductor, según sus costos reales. */
data class DriverCosts(
    val costPerKm: Double,
    val costPerMinute: Double,
    val costPerTrip: Double,
    val currency: String,
)
