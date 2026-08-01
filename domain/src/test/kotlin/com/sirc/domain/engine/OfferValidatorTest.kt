package com.sirc.domain.engine

import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import com.sirc.domain.model.ValidationIssue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferValidatorTest {
    private val validator = OfferValidator()

    @Test
    fun `oferta coherente pasa la validacion`() {
        val result = validator.validate(offer(), metrics())

        assertTrue(result.isValid)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `total ausente detecta problema de monto`() {
        val result = validator.validate(offer(estimatedTotal = null), metrics())

        assertFalse(result.isValid)
        assertTrue(result.issues.contains(ValidationIssue.INVALID_TOTAL))
    }

    @Test
    fun `distancia ausente o invalida detecta problema de distancia`() {
        val noDistance = validator.validate(offer(distanceKm = null), metrics())

        assertTrue(noDistance.issues.contains(ValidationIssue.INVALID_DISTANCE))

        val zeroDistance = validator.validate(offer(distanceKm = 0.0), metrics())
        assertTrue(zeroDistance.issues.contains(ValidationIssue.INVALID_DISTANCE))
    }

    @Test
    fun `duracion ausente o invalida detecta problema de duracion`() {
        val noDuration = validator.validate(offer(durationMin = null), metrics())

        assertTrue(noDuration.issues.contains(ValidationIssue.INVALID_DURATION))

        val negativeDuration = validator.validate(offer(durationMin = -5.0), metrics())
        assertTrue(negativeDuration.issues.contains(ValidationIssue.INVALID_DURATION))
    }

    @Test
    fun `monto negativo detecta problema de total`() {
        val result = validator.validate(offer(estimatedTotal = -10.0), metrics())

        assertTrue(result.issues.contains(ValidationIssue.NEGATIVE_TOTAL))
    }

    @Test
    fun `precio por km absurdo detecta problema de coherencia`() {
        val metrics =
            metrics().copy(
                profitPerKm = 900.0,
                profitPerHour = 120.0,
            )

        val result = validator.validate(offer(), metrics)

        assertTrue(result.issues.contains(ValidationIssue.UNREASONABLE_PRICE_PER_KM))
        assertFalse(result.issues.contains(ValidationIssue.UNREASONABLE_PRICE_PER_HOUR))
    }

    @Test
    fun `precio por hora absurdo detecta problema de coherencia`() {
        val metrics =
            metrics().copy(
                profitPerKm = 6.0,
                profitPerHour = 9_000.0,
            )

        val result = validator.validate(offer(), metrics)

        assertTrue(result.issues.contains(ValidationIssue.UNREASONABLE_PRICE_PER_HOUR))
    }

    @Test
    fun `recogida mas lejos que el viaje detecta incoherencia`() {
        val result =
            validator.validate(
                offer(distanceKm = 10.0, pickupKm = 25.0),
                metrics(),
            )

        assertTrue(result.issues.contains(ValidationIssue.PICKUP_FARTHER_THAN_TRIP))
    }

    @Test
    fun `recogida menor a la distancia es valida`() {
        val result =
            validator.validate(
                offer(distanceKm = 10.0, pickupKm = 2.0),
                metrics(),
            )

        assertFalse(result.issues.contains(ValidationIssue.PICKUP_FARTHER_THAN_TRIP))
    }

    @Test
    fun `requerimientos parciales permiten omitir chequeos de completitud`() {
        val result =
            validator.validate(
                offer(distanceKm = null, durationMin = null),
                metrics(),
                requireDistance = false,
                requireDuration = false,
            )

        assertFalse(result.issues.contains(ValidationIssue.INVALID_DISTANCE))
        assertFalse(result.issues.contains(ValidationIssue.INVALID_DURATION))
    }

    private fun offer(
        estimatedTotal: Double? = 100.0,
        distanceKm: Double? = 10.0,
        durationMin: Double? = 30.0,
        pickupKm: Double? = 2.0,
    ): TripOffer =
        TripOffer(
            platform = RidePlatform.UBER,
            timestampMillis = 1_700_000_000_000,
            estimatedTotal = estimatedTotal,
            distanceKm = distanceKm,
            durationMin = durationMin,
            pickupDistanceKm = pickupKm,
            currency = "MXN",
        )

    private fun metrics(): ProfitMetrics =
        ProfitMetrics(
            estimatedTotal = 100.0,
            distanceKm = 10.0,
            durationMin = 30.0,
            totalCost = 40.0,
            estimatedProfit = 60.0,
            profitPerKm = 6.0,
            profitPerHour = 120.0,
            marginPercent = 60.0,
        )
}
