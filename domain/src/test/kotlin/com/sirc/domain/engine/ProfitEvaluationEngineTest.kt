package com.sirc.domain.engine

import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.DriverProfile
import com.sirc.domain.model.DriverVehicle
import com.sirc.domain.model.FuelType
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitEvaluationEngineTest {
    private val engine = ProfitEvaluationEngine(engine = ProfitEngine())

    @Test
    fun `deriva el costo por km desde la configuración`() {
        val config = config(fuelPrice = 24.0, consumptionKmPerUnit = 12.0, maintenance = 0.5)

        assertEquals(2.5, ProfitEvaluationEngine.driverCosts(config).costPerKm, 0.001)
        assertEquals(2.0, ProfitEvaluationEngine.fuelCostPerKm(config), 0.001)
    }

    @Test
    fun `suma los costos adicionales al costo por km`() {
        val config =
            config(
                fuelPrice = 24.0,
                consumptionKmPerUnit = 12.0,
                maintenance = 0.5,
                additional = listOf(AdditionalCost(label = "Peajes", costPerKm = 0.4)),
            )

        assertEquals(2.9, ProfitEvaluationEngine.driverCosts(config).costPerKm, 0.001)
    }

    @Test
    fun `consumo cero no produce costo de combustible infinito`() {
        val config = config(fuelPrice = 24.0, consumptionKmPerUnit = 0.0, maintenance = 0.5)

        assertEquals(0.0, ProfitEvaluationEngine.fuelCostPerKm(config), 0.001)
        assertEquals(0.5, ProfitEvaluationEngine.driverCosts(config).costPerKm, 0.001)
    }

    @Test
    fun `evalúa con la configuración y desglosa los costos`() {
        val config =
            config(
                fuelPrice = 24.0,
                consumptionKmPerUnit = 12.0,
                maintenance = 0.5,
                additional = listOf(AdditionalCost(label = "Estacionamiento", costPerKm = 0.1)),
            )
        val offer = offer(total = 120.0, distanceKm = 10.0, durationMin = 30.0)

        val detailed = engine.evaluate(offer, config)
        val evaluation = detailed.evaluation

        // costPerKm = 2 + 0.5 + 0.1 = 2.6; costo fijo 1.0; costo/min 0.3
        assertEquals(1.0 + 10.0 * 2.6 + 30.0 * 0.3, evaluation.metrics.totalCost, 0.001)
        assertEquals(10.0 * 2.0, detailed.breakdown.fuelCost, 0.001)
        assertEquals(10.0 * 0.5, detailed.breakdown.vehicleCost, 0.001)
        assertEquals(10.0 * 0.1, detailed.breakdown.operatingCost, 0.001)
        assertEquals(evaluation.metrics.totalCost, detailed.breakdown.totalCost, 0.001)
        assertTrue(evaluation.metrics.estimatedProfit > 0.0)
    }

    @Test
    fun `usa los umbrales de la configuración para decidir`() {
        val config = config(thresholds = DecisionThresholds(minProfitPerKm = 10.0, minProfitPerHour = 500.0))
        val offer = offer(total = 120.0, distanceKm = 10.0, durationMin = 30.0)

        // Ganancia ~87, /km 8.7 < 10 => MARGINAL pese a ser rentable en absoluto
        val evaluation = engine.evaluate(offer, config).evaluation

        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `oferta sin datos suficientes no es evaluable`() {
        val config = config()
        val offer = offer(total = 100.0, distanceKm = 0.0, durationMin = 0.0)

        val result = runCatching { engine.evaluate(offer, config) }

        assertTrue(result.isFailure)
    }

    @Test
    fun `valores extremos de distancia no rompen el cálculo`() {
        val config = config()
        val offer = offer(total = 1_000_000.0, distanceKm = 5_000.0, durationMin = 2_000.0)

        val evaluation = engine.evaluate(offer, config).evaluation

        assertTrue(evaluation.metrics.totalCost > 0.0)
        assertTrue(evaluation.metrics.profitPerKm > 0.0)
    }

    private fun config(
        fuelPrice: Double = 24.0,
        consumptionKmPerUnit: Double = 12.0,
        maintenance: Double = 0.5,
        additional: List<AdditionalCost> = emptyList(),
        thresholds: DecisionThresholds = DecisionThresholds.default(),
    ): DriverConfig =
        DriverConfig(
            profile = DriverProfile(name = null, country = "México", city = "CDMX", currency = "MXN"),
            vehicle =
                DriverVehicle(
                    name = "Auto",
                    brand = "Marca",
                    model = "Modelo",
                    year = 2020,
                    fuelType = FuelType.GASOLINE,
                    consumptionKmPerUnit = consumptionKmPerUnit,
                ),
            costs = DriverCosts(costPerKm = 1.0, costPerMinute = 0.3, costPerTrip = 1.0),
            fuelPrice = fuelPrice,
            maintenanceCostPerKm = maintenance,
            additionalCosts = additional,
            platforms = setOf(RidePlatform.UBER),
            thresholds = thresholds,
        )

    private fun offer(
        total: Double,
        distanceKm: Double,
        durationMin: Double,
    ): TripOffer =
        TripOffer(
            platform = RidePlatform.UBER,
            timestampMillis = 1_700_000_000_000,
            estimatedTotal = total,
            distanceKm = distanceKm,
            durationMin = durationMin,
            currency = "MXN",
        )
}
