package com.sirc.domain.usecase

import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {
    @Test
    fun `sin filtros devuelve todas las entradas`() {
        val entries =
            listOf(
                entry(RidePlatform.UBER, Decision.PROFITABLE),
                entry(RidePlatform.DIDI, Decision.NOT_PROFITABLE),
            )

        val result = HistoryFilter.filter(entries, HistoryFilters())

        assertEquals(2, result.size)
    }

    @Test
    fun `filtra por plataforma`() {
        val entries = listOf(entry(RidePlatform.UBER), entry(RidePlatform.DIDI))

        val result = HistoryFilter.filter(entries, HistoryFilters(platform = RidePlatform.UBER))

        assertEquals(1, result.size)
        assertEquals(RidePlatform.UBER, result.single().platform)
    }

    @Test
    fun `filtra por decision`() {
        val entries =
            listOf(
                entry(RidePlatform.UBER, Decision.PROFITABLE),
                entry(RidePlatform.UBER, Decision.NOT_PROFITABLE),
            )

        val result = HistoryFilter.filter(entries, HistoryFilters(decision = Decision.NOT_PROFITABLE))

        assertEquals(1, result.size)
        assertEquals(Decision.NOT_PROFITABLE, result.single().decision)
    }

    @Test
    fun `filtra por rango de fechas`() {
        val entries =
            listOf(
                entry(RidePlatform.UBER, timestampMillis = 1_000),
                entry(RidePlatform.UBER, timestampMillis = 5_000),
                entry(RidePlatform.UBER, timestampMillis = 9_000),
            )

        val result = HistoryFilter.filter(entries, HistoryFilters(dateFromMillis = 2_000, dateToMillis = 8_000))

        assertEquals(1, result.size)
        assertEquals(5_000, result.single().timestampMillis)
    }

    @Test
    fun `busca por texto en el resumen`() {
        val entries =
            listOf(
                entry(RidePlatform.UBER, timestampMillis = 1_000, summary = "Aeropuerto aceptado"),
                entry(RidePlatform.UBER, timestampMillis = 2_000, summary = "Centro rechazado"),
            )

        val result = HistoryFilter.filter(entries, HistoryFilters(query = "aeropuerto"))

        assertEquals(1, result.size)
        assertEquals(1_000, result.single().timestampMillis)
    }

    @Test
    fun `busca ignorando mayusculas y espacios`() {
        val entries = listOf(entry(RidePlatform.UBER, summary = "Viaje Largo"))

        val result = HistoryFilter.filter(entries, HistoryFilters(query = "  largo "))

        assertEquals(1, result.size)
    }

    @Test
    fun `combina plataforma decision y busqueda`() {
        val entries =
            listOf(
                entry(RidePlatform.UBER, Decision.PROFITABLE, summary = "Aeropuerto"),
                entry(RidePlatform.UBER, Decision.NOT_PROFITABLE, summary = "Aeropuerto"),
                entry(RidePlatform.DIDI, Decision.PROFITABLE, summary = "Aeropuerto"),
            )

        val result =
            HistoryFilter.filter(
                entries,
                HistoryFilters(platform = RidePlatform.UBER, decision = Decision.PROFITABLE, query = "aeropuerto"),
            )

        assertEquals(1, result.size)
    }

    @Test
    fun `isActive es verdadero cuando hay algun criterio`() {
        assertFalse(HistoryFilters().isActive)
        assertTrue(HistoryFilters(platform = RidePlatform.UBER).isActive)
        assertTrue(HistoryFilters(query = "x").isActive)
    }

    private fun entry(
        platform: RidePlatform = RidePlatform.UBER,
        decision: Decision = Decision.PROFITABLE,
        timestampMillis: Long = System.currentTimeMillis(),
        summary: String = "Viaje aceptado",
    ): OfferHistoryEntry =
        OfferHistoryEntry(
            platform = platform,
            timestampMillis = timestampMillis,
            estimatedTotal = 125.0,
            distanceKm = 8.0,
            durationMin = 20.0,
            estimatedProfit = 100.0,
            decision = decision,
            summary = summary,
            confidencePercent = 85,
            confidenceLevel = "HIGH",
            recommendation = Recommendation.ACCEPT,
            processingMillis = 12.5,
        )
}
