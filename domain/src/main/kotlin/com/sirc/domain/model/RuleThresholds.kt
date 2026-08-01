package com.sirc.domain.model

/**
 * Límites usados por el [com.sirc.domain.engine.RuleEngine].
 *
 * Combina los umbrales de rentabilidad ([DecisionThresholds]) con límites
 * operativos del viaje (distancia máxima, tiempo máximo y distancia de
 * recogida) que el conductor no quiere superar.
 */
data class RuleThresholds(
    val minProfit: Double,
    val minProfitPerKm: Double,
    val minProfitPerHour: Double,
    val maxDistanceKm: Double,
    val maxPickupKm: Double,
    val maxTripTimeMin: Double,
) {
    companion object {
        /** Derivada de la configuración del conductor, con límites por defecto. */
        fun from(config: DriverConfig): RuleThresholds =
            RuleThresholds(
                minProfit = DEFAULT_MIN_PROFIT,
                minProfitPerKm = config.thresholds.minProfitPerKm,
                minProfitPerHour = config.thresholds.minProfitPerHour,
                maxDistanceKm = DEFAULT_MAX_DISTANCE_KM,
                maxPickupKm = DEFAULT_MAX_PICKUP_KM,
                maxTripTimeMin = DEFAULT_MAX_TRIP_TIME_MIN,
            )

        fun default(): RuleThresholds =
            RuleThresholds(
                minProfit = DEFAULT_MIN_PROFIT,
                minProfitPerKm = DEFAULT_MIN_PROFIT_PER_KM,
                minProfitPerHour = DEFAULT_MIN_PROFIT_PER_HOUR,
                maxDistanceKm = DEFAULT_MAX_DISTANCE_KM,
                maxPickupKm = DEFAULT_MAX_PICKUP_KM,
                maxTripTimeMin = DEFAULT_MAX_TRIP_TIME_MIN,
            )

        const val DEFAULT_MIN_PROFIT = 0.0
        const val DEFAULT_MIN_PROFIT_PER_KM = 4.0
        const val DEFAULT_MIN_PROFIT_PER_HOUR = 120.0
        const val DEFAULT_MAX_DISTANCE_KM = 60.0
        const val DEFAULT_MAX_PICKUP_KM = 10.0
        const val DEFAULT_MAX_TRIP_TIME_MIN = 180.0
    }
}
