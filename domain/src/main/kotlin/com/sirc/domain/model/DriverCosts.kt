package com.sirc.domain.model

/**
 * Costos unitarios del conductor usados por el motor actual.
 *
 * El costo por kilómetro se calcula actualmente de forma manual; el motor
 * futuro lo derivará de combustible + mantenimiento + costos adicionales
 * ([DriverConfig]).
 */
data class DriverCosts(
    val costPerKm: Double,
    val costPerMinute: Double,
    val costPerTrip: Double,
) {
    companion object {
        fun default(): DriverCosts = DriverCosts(costPerKm = 2.0, costPerMinute = 0.3, costPerTrip = 1.0)
    }
}
