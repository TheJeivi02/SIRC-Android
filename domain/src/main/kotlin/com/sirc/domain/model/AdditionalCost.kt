package com.sirc.domain.model

/**
 * Costo adicional configurable por kilómetro (p. ej. peajes, estacionamiento).
 *
 * La lista es extensible: el motor futuro podrá sumar todos los costos
 * adicionales del conductor.
 */
data class AdditionalCost(
    val label: String,
    val costPerKm: Double,
)
