package com.sirc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "overlay_config")
data class OverlayConfigEntity(
    @PrimaryKey val id: Int = 1,
    val showDecision: Boolean,
    val showProfit: Boolean,
    val showProfitPerHour: Boolean,
    val showProfitPerKm: Boolean,
    val showTripSummary: Boolean,
    val compactMode: Boolean,
    val opacityPercent: Int,
    val ttlSeconds: Long,
    val positionXPercent: Float,
    val positionYPercent: Float,
)
