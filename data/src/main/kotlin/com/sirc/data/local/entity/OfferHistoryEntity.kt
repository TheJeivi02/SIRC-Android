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
    val offerType: String? = null,
    val confidencePercent: Int? = null,
    val confidenceLevel: String? = null,
    val ruleSummary: String? = null,
    val reasons: String? = null,
    val recommendation: String? = null,
    val processingMillis: Double? = null,
    val evaluationMillis: Double? = null,
    val rulesMillis: Double? = null,
)
