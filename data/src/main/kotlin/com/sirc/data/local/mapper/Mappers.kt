package com.sirc.data.local.mapper

import com.sirc.data.local.entity.DriverConfigEntity
import com.sirc.data.local.entity.OfferHistoryEntity
import com.sirc.data.local.entity.OverlayConfigEntity
import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.DriverProfile
import com.sirc.domain.model.DriverVehicle
import com.sirc.domain.model.FuelType
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform

private const val ITEM_SEPARATOR = "\u001E"
private const val FIELD_SEPARATOR = "\u001F"

fun DriverConfigEntity.toDriverConfig(): DriverConfig =
    DriverConfig(
        profile = DriverProfile(name = name, country = country, city = city, currency = currency),
        vehicle =
            DriverVehicle(
                name = vehicleName,
                brand = brand,
                model = model,
                year = year,
                fuelType = FuelType.entries.firstOrNull { it.name == fuelType } ?: FuelType.GASOLINE,
                consumptionKmPerUnit = consumptionKmPerUnit,
            ),
        costs = DriverCosts(costPerKm = costPerKm, costPerMinute = costPerMinute, costPerTrip = costPerTrip),
        fuelPrice = fuelPrice,
        maintenanceCostPerKm = maintenanceCostPerKm,
        additionalCosts = decodeAdditionalCosts(additionalCosts),
        platforms = decodePlatforms(platforms),
        thresholds = DecisionThresholds(minProfitPerKm = minProfitPerKm, minProfitPerHour = minProfitPerHour),
    )

fun DriverConfig.toEntity(): DriverConfigEntity =
    DriverConfigEntity(
        costPerKm = costs.costPerKm,
        costPerMinute = costs.costPerMinute,
        costPerTrip = costs.costPerTrip,
        name = profile.name,
        country = profile.country,
        city = profile.city,
        currency = profile.currency,
        vehicleName = vehicle.name,
        brand = vehicle.brand,
        model = vehicle.model,
        year = vehicle.year,
        fuelType = vehicle.fuelType.name,
        consumptionKmPerUnit = vehicle.consumptionKmPerUnit,
        fuelPrice = fuelPrice,
        maintenanceCostPerKm = maintenanceCostPerKm,
        additionalCosts = encodeAdditionalCosts(additionalCosts),
        platforms = encodePlatforms(platforms),
        minProfitPerKm = thresholds.minProfitPerKm,
        minProfitPerHour = thresholds.minProfitPerHour,
    )

/** Codifica las plataformas activas como nombres separados por coma (orden estable). */
fun encodePlatforms(platforms: Set<RidePlatform>): String = platforms.map { it.name }.sorted().joinToString(",")

/** Decodifica el texto persistido de plataformas activas. */
fun decodePlatforms(value: String): Set<RidePlatform> =
    value
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { RidePlatform.entries.firstOrNull { platform -> platform.name == it } }
        .toSet()

/** Codifica los costos adicionales: item `label\u001Famount`, items separados por `\u001E`. */
fun encodeAdditionalCosts(costs: List<AdditionalCost>): String =
    costs
        .filter { it.label.isNotBlank() }
        .joinToString(ITEM_SEPARATOR) { cost -> "${cost.label}$FIELD_SEPARATOR${cost.costPerKm}" }

/** Decodifica el texto persistido de costos adicionales. */
fun decodeAdditionalCosts(value: String): List<AdditionalCost> {
    if (value.isBlank()) return emptyList()
    return value.split(ITEM_SEPARATOR).mapNotNull { item ->
        val fields = item.split(FIELD_SEPARATOR)
        if (fields.size != 2) return@mapNotNull null
        val amount = fields[1].toDoubleOrNull() ?: return@mapNotNull null
        AdditionalCost(label = fields[0], costPerKm = amount)
    }
}

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
        historyLimit = historyLimit,
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
        historyLimit = historyLimit,
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
        offerType = offerType,
        confidencePercent = confidencePercent,
        confidenceLevel = confidenceLevel,
        ruleSummary = ruleSummary,
        reasons = reasons,
        recommendation = recommendation?.name,
        processingMillis = processingMillis,
        evaluationMillis = evaluationMillis,
        rulesMillis = rulesMillis,
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
        offerType = offerType,
        confidencePercent = confidencePercent,
        confidenceLevel = confidenceLevel,
        ruleSummary = ruleSummary,
        reasons = reasons,
        recommendation = recommendation?.let { Recommendation.valueOf(it) },
        processingMillis = processingMillis,
        evaluationMillis = evaluationMillis,
        rulesMillis = rulesMillis,
    )
