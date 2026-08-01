package com.sirc.feature.overlay

import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferEvaluationRecord
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryOfferEvaluationRepositoryTest {
    private val repository = InMemoryOfferEvaluationRepository()

    @Test
    fun `agrega registros y los observa en orden`() =
        runBlocking {
            repository.add(record(price = 100.0))
            repository.add(record(price = 200.0))

            val records = repository.observe(limit = 10).first()

            assertEquals(2, records.size)
            assertEquals(100.0, records[0].price, 0.001)
            assertEquals(200.0, records[1].price, 0.001)
        }

    @Test
    fun `asigna ids incrementales`() =
        runBlocking {
            repository.add(record())
            repository.add(record())

            val records = repository.observe(limit = 10).first()

            assertEquals(1L, records[0].id)
            assertEquals(2L, records[1].id)
        }

    @Test
    fun `conserva a lo sumo 100 registros`() =
        runBlocking {
            repeat(150) { index ->
                repository.add(record(price = index.toDouble()))
            }

            val records = repository.observe(limit = 100).first()

            assertEquals(100, records.size)
            assertEquals(50.0, records.first().price, 0.001)
        }

    @Test
    fun `clear vacía el historial`() =
        runBlocking {
            repository.add(record())

            repository.clear()

            assertTrue(repository.observe(limit = 10).first().isEmpty())
        }

    @Test
    fun `limit acota el resultado a las últimas entradas`() =
        runBlocking {
            repository.add(record(price = 1.0))
            repository.add(record(price = 2.0))
            repository.add(record(price = 3.0))

            val records = repository.observe(limit = 2).first()

            assertEquals(2, records.size)
            assertEquals(2.0, records[0].price, 0.001)
            assertEquals(3.0, records[1].price, 0.001)
        }

    private fun record(price: Double = 100.0): OfferEvaluationRecord {
        val offer =
            TripOffer(
                platform = RidePlatform.UBER,
                timestampMillis = 1_700_000_000_000,
                estimatedTotal = price,
                distanceKm = 8.5,
                durationMin = 22.0,
                rawText = listOf("UBER", "$125.00"),
            )
        val metrics =
            ProfitMetrics(
                estimatedTotal = price,
                distanceKm = 8.5,
                durationMin = 22.0,
                totalCost = 28.85,
                estimatedProfit = 71.15,
                profitPerKm = 8.37,
                profitPerHour = 194.0,
                marginPercent = 71.0,
            )
        return OfferEvaluationRecord(
            id = 0L,
            timestampMillis = 1_700_000_000_000,
            platform = RidePlatform.UBER,
            price = price,
            distanceKm = 8.5,
            durationMin = 22.0,
            ocrText = listOf("UBER", "$125.00", "8.5 km"),
            parserResult = "data:simulated",
            evaluation =
                ProfitEvaluation(
                    offer = offer,
                    metrics = metrics,
                    decision = Decision.PROFITABLE,
                    reasons = listOf("Supera los umbrales configurados"),
                ),
            recommendation = Recommendation.ACCEPT,
            confidencePercent = 90,
        )
    }
}
