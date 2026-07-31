package com.sirc.domain.model

/**
 * Vehículo del conductor.
 *
 * @param name nombre asignado al vehículo (p. ej. "Mi auto").
 * @param brand marca.
 * @param model modelo.
 * @param year año del modelo.
 * @param fuelType tipo de combustible/energía.
 * @param consumptionKmPerUnit consumo por unidad: km/L (combustión), km/kWh (eléctrico).
 */
data class DriverVehicle(
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    val fuelType: FuelType,
    val consumptionKmPerUnit: Double,
)
