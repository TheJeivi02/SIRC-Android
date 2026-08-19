package com.sirc.domain.engine

import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.ProfitBreakdown
import com.sirc.domain.model.ProfitEvaluationDetailed
import com.sirc.domain.model.TripOffer
import javax.inject.Inject

/**
 * Motor de Rentabilidad basado en la [DriverConfig].
 *
 * Deriva los costos unitarios desde la configuración completa del conductor
 * (combustible + mantenimiento + costos adicionales, sin constantes) y delega
 * en el [ProfitEngine] para las métricas y la decisión, sin duplicar cálculos.
 */
class ProfitEvaluationEngine @Inject constructor(
    private val engine: ProfitEngine,
) {
    /**
     * Evalúa [offer] usando exclusivamente los valores de [config].
     *
     * @throws IllegalArgumentException si la oferta no tiene datos suficientes.
     */
    fun evaluate(
        offer: TripOffer,
        config: DriverConfig,
    ): ProfitEvaluationDetailed {
        val costs = driverCosts(config)
        val evaluation = engine.evaluate(offer, costs, config.thresholds)
        val distance = evaluation.metrics.distanceKm
        val breakdown =
            ProfitBreakdown(
                fuelCost = distance * fuelCostPerKm(config),
                vehicleCost = distance * config.maintenanceCostPerKm,
                operatingCost = distance * config.additionalCosts.sumOf { it.costPerKm },
                totalCost = evaluation.metrics.totalCost,
            )
        return ProfitEvaluationDetailed(evaluation = evaluation, breakdown = breakdown)
    }

    companion object {
        /** Costo de combustible por kilómetro (precio / consumo del vehículo). */
        fun fuelCostPerKm(config: DriverConfig): Double =
            if (config.vehicle.consumptionKmPerUnit > 0.0) {
                config.fuelPrice / config.vehicle.consumptionKmPerUnit
            } else {
                0.0
            }

        /**
         * Deriva los [DriverCosts] a partir de la [DriverConfig]: el costo por
         * kilómetro se compone de combustible + mantenimiento + adicionales y
         * el costo fijo por viaje pasa de la configuración. No existe costo
         * por minuto en el modelo económico aprobado.
         */
        fun driverCosts(config: DriverConfig): DriverCosts {
            val costPerKm =
                fuelCostPerKm(config) +
                    config.maintenanceCostPerKm +
                    config.additionalCosts.sumOf { it.costPerKm }
            return DriverCosts(
                costPerKm = costPerKm,
                costPerTrip = config.costs.costPerTrip,
            )
        }
    }
}
