package com.sirc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offer_history")
data class OfferHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val platform: String,
    val timestampMillis: Long,
    val estimatedTotal: Double?,
    val distanceKm: Double?,
    val durationMin: Double?,
    val estimatedProfit: Double,
    val decision: String,
    val summary: String,
)
