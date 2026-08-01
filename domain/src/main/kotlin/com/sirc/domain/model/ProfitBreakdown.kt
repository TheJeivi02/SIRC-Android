package com.sirc.domain.model

/**
 * Desglose de costos estimados de una oferta.
 *
 * [fuelCost], [vehicleCost] y [operatingCost] cubren el componente por
 * kilómetro (combustible + mantenimiento + costos adicionales); [totalCost]
 * es el costo total del viaje (incluye el componente por tiempo y el costo
 * fijo por viaje), calculado por el motor.
 */
data class ProfitBreakdown(
    val fuelCost: Double,
    val vehicleCost: Double,
    val operatingCost: Double,
    val totalCost: Double,
)
