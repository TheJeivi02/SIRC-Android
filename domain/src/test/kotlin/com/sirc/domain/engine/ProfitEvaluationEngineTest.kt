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
import org.junit.Assert.assertNull
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
    fun `el costo por km manual de la configuracion no altera el derivado`() {
        val base = config(fuelPrice = 24.0, consumptionKmPerUnit = 12.0, maintenance = 0.5)
        val withManual = base.copy(costs = base.costs.copy(costPerKm = 999.0))

        assertEquals(2.5, ProfitEvaluationEngine.driverCosts(withManual).costPerKm, 0.001)
    }

    @Test
    fun `cambiar los componentes actualiza el costo por km derivado`() {
        val base = config(fuelPrice = 24.0, consumptionKmPerUnit = 12.0, maintenance = 0.5)
        val expensive =
            base.copy(
                fuelPrice = 48.0,
                maintenanceCostPerKm = 1.5,
                additionalCosts = listOf(AdditionalCost(label = "Peajes", costPerKm = 0.6)),
            )

        assertEquals(2.5, ProfitEvaluationEngine.driverCosts(base).costPerKm, 0.001)
        // 48/12 + 1.5 + 0.6 = 4.0 + 1.5 + 0.6 = 6.1
        assertEquals(6.1, ProfitEvaluationEngine.driverCosts(expensive).costPerKm, 0.001)
    }

    @Test
    fun `un aumento de costos cambia la decision de la evaluacion`() {
        val offer = offer(total = 120.0, distanceKm = 10.0, durationMin = 30.0)
        val cheap = config(fuelPrice = 24.0, consumptionKmPerUnit = 12.0, maintenance = 0.5)
        val costly =
            cheap.copy(
                fuelPrice = 24.0,
                maintenanceCostPerKm = 4.0,
                vehicle = cheap.vehicle.copy(consumptionKmPerUnit = 4.0),
            )

        val cheapDecision = engine.evaluate(offer, cheap).evaluation.decision
        val costlyDecision = engine.evaluate(offer, costly).evaluation.decision

        // Con costos bajos (2.5/km) la oferta es rentable; con costos altos
        // (6+4=10/km) la misma oferta deja de serlo.
        assertEquals(Decision.PROFITABLE, cheapDecision)
        assertTrue(costlyDecision != Decision.PROFITABLE)
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

        // costPerKm = 2 + 0.5 + 0.1 = 2.6; costo fijo 1.0; SIN costo por minuto.
        assertEquals(1.0 + 10.0 * 2.6, evaluation.metrics.totalCost, 0.001)
        assertEquals(10.0 * 2.0, detailed.breakdown.fuelCost, 0.001)
        assertEquals(10.0 * 0.5, detailed.breakdown.vehicleCost, 0.001)
        assertEquals(10.0 * 0.1, detailed.breakdown.operatingCost, 0.001)
        assertEquals(evaluation.metrics.totalCost, detailed.breakdown.totalCost, 0.001)
        assertTrue(evaluation.metrics.estimatedProfit > 0.0)
    }

    @Test
    fun `el costo fijo por viaje se traslada al motor y la duracion no aporta costo`() {
        val config = config(costPerTrip = 2.5)
        val offer = offer(total = 120.0, distanceKm = 10.0, durationMin = 30.0)

        val evaluation = engine.evaluate(offer, config).evaluation

        // costPerKm derivado = 24/12 + 0.5 = 2.5; fijo 2.5; la duración no suma.
        assertEquals(2.5 + 10.0 * 2.5, evaluation.metrics.totalCost, 0.001)
    }

    @Test
    fun `caso real obligatorio con motor completo no es perdida`() {
        // Referencia de auditoría: Ecuador/USD, costo/km 0.50, fijo $0, objetivo $11/h.
        val config =
            config(
                fuelPrice = 6.0,
                consumptionKmPerUnit = 12.0,
                maintenance = 0.0,
                costPerTrip = 0.0,
                thresholds = DecisionThresholds(minProfitPerKm = 0.5, minProfitPerHour = 11.0),
            )
        val offer = offer(total = 5.90, distanceKm = 0.0, durationMin = 27.0)

        val evaluation = engine.evaluate(offer, config).evaluation

        assertEquals(Decision.MARGINAL, evaluation.decision)
        assertTrue(evaluation.metrics.estimatedProfit >= 0.0)
        // La duración (27 min) sí está: la ganancia por hora se calcula.
        assertEquals(5.9 / 0.45, evaluation.metrics.profitPerHour ?: -1.0, 0.001)
        assertNull(evaluation.metrics.profitPerKm)
    }

    @Test
    fun `usa los umbrales de la configuración para decidir`() {
        val config = config(thresholds = DecisionThresholds(minProfitPerKm = 10.0, minProfitPerHour = 500.0))
        val offer = offer(total = 120.0, distanceKm = 10.0, durationMin = 30.0)

        // Ganancia ~93, /km 9.3 < 10 => MARGINAL pese a ser rentable en absoluto.
        val evaluation = engine.evaluate(offer, config).evaluation

        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `precio sin distancia ni duracion se evalua sin inventar metricas`() {
        val config = config()
        val offer = offer(total = 100.0, distanceKm = 0.0, durationMin = 0.0)

        val evaluation = engine.evaluate(offer, config).evaluation

        // Costo fijo 1.0; sin distancia ni duración no hay métricas derivadas.
        assertEquals(1.0, evaluation.metrics.totalCost, 0.001)
        assertEquals(99.0, evaluation.metrics.estimatedProfit, 0.001)
        assertNull(evaluation.metrics.profitPerKm)
        assertNull(evaluation.metrics.profitPerHour)
        assertEquals(Decision.MARGINAL, evaluation.decision)
    }

    @Test
    fun `oferta sin monto no es evaluable`() {
        val config = config()
        val offer =
            TripOffer(
                platform = RidePlatform.UBER,
                timestampMillis = 1_700_000_000_000,
                estimatedTotal = null,
                fareAmount = null,
                distanceKm = 10.0,
                durationMin = 30.0,
            )

        val result = runCatching { engine.evaluate(offer, config) }

        assertTrue(result.isFailure)
    }

    @Test
    fun `valores extremos de distancia no rompen el cálculo`() {
        val config = config()
        val offer = offer(total = 1_000_000.0, distanceKm = 5_000.0, durationMin = 2_000.0)

        val evaluation = engine.evaluate(offer, config).evaluation

        assertTrue(evaluation.metrics.totalCost > 0.0)
        assertTrue((evaluation.metrics.profitPerKm ?: -1.0) > 0.0)
    }

    private fun config(
        fuelPrice: Double = 24.0,
        consumptionKmPerUnit: Double = 12.0,
        maintenance: Double = 0.5,
        additional: List<AdditionalCost> = emptyList(),
        costPerTrip: Double = 1.0,
        thresholds: DecisionThresholds = DecisionThresholds.default(),
    ): DriverConfig =
        DriverConfig(
            profile = DriverProfile(name = null, country = "Ecuador", city = "Quito", currency = "USD"),
            vehicle =
                DriverVehicle(
                    name = "Auto",
                    brand = "Toyota",
                    model = "Corolla",
                    year = 2021,
                    fuelType = FuelType.GASOLINE,
                    consumptionKmPerUnit = consumptionKmPerUnit,
                ),
            costs = DriverCosts(costPerKm = 1.0, costPerTrip = costPerTrip),
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
            currency = "USD",
        )
}
