package com.sirc.domain.model

/**
 * Configuración completa del conductor, persistida en una única fila Room.
 *
 * Es la única fuente de verdad para el futuro motor de rentabilidad: perfil,
 * vehículo, costos (combustible, mantenimiento, adicionales), plataformas
 * activas y objetivos de rentabilidad.
 */
data class DriverConfig(
    val profile: DriverProfile,
    val vehicle: DriverVehicle,
    val costs: DriverCosts,
    val fuelPrice: Double,
    val maintenanceCostPerKm: Double,
    val additionalCosts: List<AdditionalCost>,
    val platforms: Set<RidePlatform>,
    val thresholds: DecisionThresholds,
) {
    val isConfigured: Boolean
        get() =
            profile.country.isNotBlank() &&
                profile.city.isNotBlank() &&
                vehicle.brand.isNotBlank() &&
                vehicle.model.isNotBlank() &&
                platforms.isNotEmpty()

    companion object {
        /** Configuración vacía para el onboarding (sin datos obligatorios). */
        fun blank(): DriverConfig =
            DriverConfig(
                profile = DriverProfile(name = null, country = "", city = "", currency = "MXN"),
                vehicle =
                    DriverVehicle(
                        name = "",
                        brand = "",
                        model = "",
                        year = 2020,
                        fuelType = FuelType.GASOLINE,
                        consumptionKmPerUnit = 12.0,
                    ),
                costs = DriverCosts.default(),
                fuelPrice = 24.0,
                maintenanceCostPerKm = 0.5,
                additionalCosts = emptyList(),
                platforms = emptySet(),
                thresholds = DecisionThresholds.default(),
            )

        /** Configuración con valores por defecto razonables (respaldo legado). */
        fun default(): DriverConfig =
            blank().copy(
                profile = blank().profile.copy(country = "México", city = "Ciudad de México"),
                vehicle = blank().vehicle.copy(name = "Mi vehículo"),
                platforms = RidePlatform.entries.toSet(),
            )
    }
}
