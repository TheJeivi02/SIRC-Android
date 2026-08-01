package com.sirc.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sirc.data.local.dao.OfferHistoryDao
import com.sirc.data.local.entity.OfferHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfferHistoryDaoTest {
    private lateinit var database: SircDatabase
    private lateinit var dao: OfferHistoryDao

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                SircDatabase::class.java,
            ).allowMainThreadQueries().build()
        dao = database.offerHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertYObserveDevuelveEntradasOrdenadas() =
        runBlocking {
            dao.insert(entity(timestamp = 1_000))
            dao.insert(entity(timestamp = 2_000))

            val entries = dao.observeEntries(limit = 10).first()

            assertEquals(2, entries.size)
            assertEquals(2_000, entries[0].timestampMillis)
            assertEquals(1_000, entries[1].timestampMillis)
        }

    @Test
    fun observeRespetaElLimite() =
        runBlocking {
            dao.insert(entity(timestamp = 1_000))
            dao.insert(entity(timestamp = 2_000))
            dao.insert(entity(timestamp = 3_000))

            val entries = dao.observeEntries(limit = 2).first()

            assertEquals(2, entries.size)
            assertEquals(3_000, entries[0].timestampMillis)
        }

    @Test
    fun trimToLimitEliminaLosMasAntiguos() =
        runBlocking {
            dao.insert(entity(timestamp = 1_000))
            dao.insert(entity(timestamp = 2_000))
            dao.insert(entity(timestamp = 3_000))

            dao.trimToLimit(limit = 2)

            assertEquals(2, dao.count())
            val entries = dao.observeEntries(limit = 10).first()
            assertTrue(entries.none { it.timestampMillis == 1_000L })
            assertTrue(entries.any { it.timestampMillis == 2_000L })
            assertTrue(entries.any { it.timestampMillis == 3_000L })
        }

    @Test
    fun trimToLimitConLimiteMayorNoEliminaNada() =
        runBlocking {
            dao.insert(entity(timestamp = 1_000))

            dao.trimToLimit(limit = 100)

            assertEquals(1, dao.count())
        }

    @Test
    fun clearEliminaTodo() =
        runBlocking {
            dao.insert(entity(timestamp = 1_000))

            dao.clear()

            assertEquals(0, dao.count())
        }

    @Test
    fun insertConservaElAnalisisDetallado() =
        runBlocking {
            dao.insert(
                entity(
                    timestamp = 1_000,
                    offerType = "UBER_REQUEST",
                    confidencePercent = 85,
                    confidenceLevel = "HIGH",
                    ruleSummary = "profitPerKm:PROFITABLE",
                    reasons = "Alta ganancia",
                    recommendation = "ACCEPT",
                    processingMillis = 12.5,
                    evaluationMillis = 8.0,
                    rulesMillis = 4.0,
                ),
            )

            val entry = dao.observeEntries(limit = 1).first().single()

            assertEquals("UBER_REQUEST", entry.offerType)
            assertEquals(85, entry.confidencePercent)
            assertEquals("HIGH", entry.confidenceLevel)
            assertEquals("profitPerKm:PROFITABLE", entry.ruleSummary)
            assertEquals("Alta ganancia", entry.reasons)
            assertEquals("ACCEPT", entry.recommendation)
            assertEquals(12.5, entry.processingMillis!!, 0.001)
            assertEquals(8.0, entry.evaluationMillis!!, 0.001)
            assertEquals(4.0, entry.rulesMillis!!, 0.001)
        }

    private fun entity(
        timestamp: Long,
        offerType: String? = null,
        confidencePercent: Int? = null,
        confidenceLevel: String? = null,
        ruleSummary: String? = null,
        reasons: String? = null,
        recommendation: String? = null,
        processingMillis: Double? = null,
        evaluationMillis: Double? = null,
        rulesMillis: Double? = null,
    ): OfferHistoryEntity =
        OfferHistoryEntity(
            platform = "UBER",
            timestampMillis = timestamp,
            estimatedTotal = 125.0,
            distanceKm = 8.5,
            durationMin = 22.0,
            estimatedProfit = 100.0,
            decision = "PROFITABLE",
            summary = "Viaje aceptado",
            offerType = offerType,
            confidencePercent = confidencePercent,
            confidenceLevel = confidenceLevel,
            ruleSummary = ruleSummary,
            reasons = reasons,
            recommendation = recommendation,
            processingMillis = processingMillis,
            evaluationMillis = evaluationMillis,
            rulesMillis = rulesMillis,
        )
}
