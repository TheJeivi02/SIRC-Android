package com.sirc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_config")
data class DriverConfigEntity(
    @PrimaryKey val id: Int = 1,
    val costPerKm: Double,
    val costPerMinute: Double,
    val costPerTrip: Double,
    val currency: String,
    val minProfit: Double,
    val minProfitPerHour: Double,
)
