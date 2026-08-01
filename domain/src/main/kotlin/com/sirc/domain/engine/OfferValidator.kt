package com.sirc.domain.engine

import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.TripOffer
import com.sirc.domain.model.ValidationIssue
import com.sirc.domain.model.ValidationResult
import javax.inject.Inject

/**
 * Validador Cruzado (O4): comprueba que los datos extraídos del texto de la
 * pantalla sean internamente coherentes.
 *
 * Protege al resto del pipeline de ofertas mal parseadas: un precio/km o
 * precio/hora absurdos delatan decimales corridos o unidades equivocadas, y
 * esos casos deben tratarse como "Información insuficiente" en lugar de
 * generar una recomendación engañosa.
 */
class OfferValidator @Inject constructor() {
    /**
     * Valida [offer] y sus [metrics] derivadas.
     *
     * [requireTotal]/[requireDistance]/[requireDuration] permiten omitir los
     * chequeos de completitud cuando el consumidor no los necesita (p. ej.
     * una pantalla de viaje en curso sin total).
     */
    fun validate(
        offer: TripOffer,
        metrics: ProfitMetrics,
        requireTotal: Boolean = true,
        requireDistance: Boolean = true,
        requireDuration: Boolean = true,
    ): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        if (requireTotal) {
            val total = offer.estimatedTotal
            if (total == null || !total.isFinite()) {
                issues += ValidationIssue.INVALID_TOTAL
            } else if (total < 0.0) {
                issues += ValidationIssue.NEGATIVE_TOTAL
            }
        }

        if (requireDistance) {
            val distance = offer.distanceKm
            if (distance == null || !distance.isFinite() || distance <= 0.0) {
                issues += ValidationIssue.INVALID_DISTANCE
            }
        }

        if (requireDuration) {
            val duration = offer.durationMin
            if (duration == null || !duration.isFinite() || duration <= 0.0) {
                issues += ValidationIssue.INVALID_DURATION
            }
        }

        if (metrics.profitPerKm > MAX_REASONABLE_PRICE_PER_KM || metrics.profitPerKm < 0.0) {
            issues += ValidationIssue.UNREASONABLE_PRICE_PER_KM
        }
        if (metrics.profitPerHour > MAX_REASONABLE_PRICE_PER_HOUR || metrics.profitPerHour < 0.0) {
            issues += ValidationIssue.UNREASONABLE_PRICE_PER_HOUR
        }

        val pickup = offer.pickupDistanceKm
        val distance = offer.distanceKm
        if (pickup != null && distance != null && pickup > distance) {
            issues += ValidationIssue.PICKUP_FARTHER_THAN_TRIP
        }

        return if (issues.isEmpty()) ValidationResult.valid else ValidationResult(issues)
    }

    companion object {
        const val MAX_REASONABLE_PRICE_PER_KM = 500.0
        const val MAX_REASONABLE_PRICE_PER_HOUR = 5000.0
    }
}
