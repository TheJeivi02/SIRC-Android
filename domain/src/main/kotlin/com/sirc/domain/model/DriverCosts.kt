package com.sirc.domain.model

/**
 * Costos reales unitarios del conductor que usa el motor.
 *
 * [costPerKm] es el costo derivado por kilómetro (combustible + mantenimiento
 * + costos adicionales, ver [com.sirc.domain.engine.ProfitEvaluationEngine]);
 * [costPerTrip] es el costo fijo opcional por viaje. No existe costo por
 * minuto: el tiempo ya está representado por el objetivo de ganancia por hora.
 */
data class DriverCosts(
    val costPerKm: Double,
    val costPerTrip: Double,
) {
    companion object {
        fun default(): DriverCosts = DriverCosts(costPerKm = 2.0, costPerTrip = 0.0)
    }
}
