package com.sirc.data.local.mapper

import com.sirc.data.local.entity.DriverConfigEntity
import com.sirc.data.local.entity.OfferHistoryEntity
import com.sirc.data.local.entity.OverlayConfigEntity
import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.RidePlatform

fun DriverConfigEntity.toDriverCosts(): DriverCosts =
    DriverCosts(
        costPerKm = costPerKm,
        costPerMinute = costPerMinute,
        costPerTrip = costPerTrip,
        currency = currency,
    )

fun DriverConfigEntity.toDecisionThresholds(): DecisionThresholds =
    DecisionThresholds(
        minProfit = minProfit,
        minProfitPerHour = minProfitPerHour,
    )

fun DriverCosts.toEntity(thresholds: DecisionThresholds): DriverConfigEntity =
    DriverConfigEntity(
        costPerKm = costPerKm,
        costPerMinute = costPerMinute,
        costPerTrip = costPerTrip,
        currency = currency,
        minProfit = thresholds.minProfit,
        minProfitPerHour = thresholds.minProfitPerHour,
    )

fun OverlayConfigEntity.toDomain(): OverlayConfig =
    OverlayConfig(
        showDecision = showDecision,
        showProfit = showProfit,
        showProfitPerHour = showProfitPerHour,
        showProfitPerKm = showProfitPerKm,
        showTripSummary = showTripSummary,
        compactMode = compactMode,
        opacityPercent = opacityPercent,
        ttlSeconds = ttlSeconds,
        positionXPercent = positionXPercent,
        positionYPercent = positionYPercent,
    )

fun OverlayConfig.toEntity(): OverlayConfigEntity =
    OverlayConfigEntity(
        showDecision = showDecision,
        showProfit = showProfit,
        showProfitPerHour = showProfitPerHour,
        showProfitPerKm = showProfitPerKm,
        showTripSummary = showTripSummary,
        compactMode = compactMode,
        opacityPercent = opacityPercent,
        ttlSeconds = ttlSeconds,
        positionXPercent = positionXPercent,
        positionYPercent = positionYPercent,
    )

fun OfferHistoryEntry.toEntity(): OfferHistoryEntity =
    OfferHistoryEntity(
        id = id,
        platform = platform.name,
        timestampMillis = timestampMillis,
        estimatedTotal = estimatedTotal,
        distanceKm = distanceKm,
        durationMin = durationMin,
        estimatedProfit = estimatedProfit,
        decision = decision.name,
        summary = summary,
    )

fun OfferHistoryEntity.toDomain(): OfferHistoryEntry =
    OfferHistoryEntry(
        id = id,
        platform = RidePlatform.valueOf(platform),
        timestampMillis = timestampMillis,
        estimatedTotal = estimatedTotal,
        distanceKm = distanceKm,
        durationMin = durationMin,
        estimatedProfit = estimatedProfit,
        decision = Decision.valueOf(decision),
        summary = summary,
    )
