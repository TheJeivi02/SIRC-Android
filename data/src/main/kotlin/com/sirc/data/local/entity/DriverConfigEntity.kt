package com.sirc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila única con la configuración completa del conductor.
 *
 * Las columnas [platforms] y [additionalCosts] se persisten codificadas como
 * texto (ver `data/local/mapper/Mappers.kt`).
 */
@Entity(tableName = "driver_config")
data class DriverConfigEntity(
    @PrimaryKey val id: Int = 1,
    // Costos unitarios del motor actual
    val costPerKm: Double,
    val costPerMinute: Double,
    val costPerTrip: Double,
    // Perfil
    val name: String?,
    val country: String,
    val city: String,
    val currency: String,
    // Vehículo
    val vehicleName: String,
    val brand: String,
    val model: String,
    val year: Int,
    val fuelType: String,
    val consumptionKmPerUnit: Double,
    // Costos básicos
    val fuelPrice: Double,
    val maintenanceCostPerKm: Double,
    val additionalCosts: String,
    // Plataformas activas (nombres separados por coma)
    val platforms: String,
    // Objetivos de rentabilidad
    val minProfitPerKm: Double,
    val minProfitPerHour: Double,
)
