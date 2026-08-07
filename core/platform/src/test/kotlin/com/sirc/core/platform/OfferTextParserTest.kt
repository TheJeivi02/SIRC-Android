package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferTextParserTest {
    private val parser = OfferTextParser()

    @Test
    fun `extrae monto total con simbolo`() {
        val parsed = parser.parse(listOf("Tarifa total $120.50"))
        assertEquals(1, parsed.amounts.size)
        assertEquals(120.5, parsed.amounts.first().value, 0.001)
    }

    @Test
    fun `extrae monto con codigo de moneda`() {
        val parsed = parser.parse(listOf("Total MXN 95"))
        assertEquals(95.0, parsed.amounts.first().value, 0.001)
        assertEquals("MXN", parsed.amounts.first().currency)
    }

    @Test
    fun `extrae distancia en kilometros`() {
        val parsed = parser.parse(listOf("Distancia 8.5 km"))
        assertTrue(parsed.distancesKm.contains(8.5))
    }

    @Test
    fun `extrae duracion en minutos`() {
        val parsed = parser.parse(listOf("Tiempo estimado 25 minutos"))
        assertTrue(parsed.durationsMin.contains(25.0))
    }

    @Test
    fun `extrae duracion con horas y minutos`() {
        val parsed = parser.parse(listOf("Duración 1h 15m"))
        assertTrue(parsed.durationsMin.contains(75.0))
    }

    @Test
    fun `ignora montos absurdos`() {
        val parsed = parser.parse(listOf("Total $99999999"))
        assertTrue(parsed.amounts.isEmpty())
    }

    @Test
    fun `extractor generico reconoce oferta de uber`() {
        val extractor =
            GenericPlatformExtractor(
                RidePlatform.UBER,
                PlatformDescriptors.UBER.extractorKeywords,
                parser,
                defaultCurrency = PlatformDescriptors.UBER.defaultCurrency,
            )
        val offer =
            extractor.extract(
                listOf("Nuevo viaje", "Total $120", "8.5 km", "25 min"),
                timestampMillis = 1000L,
            )
        assertNotNull(offer)
        assertEquals(120.0, offer!!.estimatedTotal ?: 0.0, 0.001)
        assertEquals(8.5, offer.distanceKm ?: 0.0, 0.001)
        assertEquals(25.0, offer.durationMin ?: 0.0, 0.001)
    }

    @Test
    fun `extractor devuelve null sin monto`() {
        val extractor =
            GenericPlatformExtractor(
                RidePlatform.DIDI,
                PlatformDescriptors.DIDI.extractorKeywords,
                parser,
                defaultCurrency = PlatformDescriptors.DIDI.defaultCurrency,
            )
        assertNull(extractor.extract(listOf("Conductor asignado", "8.5 km", "25 min"), 1000L))
    }
}
