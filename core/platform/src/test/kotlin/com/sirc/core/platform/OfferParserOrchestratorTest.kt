package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferParserOrchestratorTest {
    private fun orchestrator(): OfferParserOrchestrator =
        OfferParserOrchestrator(
            PlatformDescriptorRegistry(listOf(PlatformDescriptors.UBER)),
        )

    @Test
    fun `solicitud estandar se detecta como UBER_REQUEST`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Nueva solicitud de viaje", "Aceptar", "Rechazar", "Total $120", "8.5 km", "25 min"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertEquals(OfferType.UBER_REQUEST, parsed.type)
        assertNotNull(parsed.offer)
        assertEquals(120.0, parsed.offer?.estimatedTotal ?: 0.0, 0.001)
    }

    @Test
    fun `radar se detecta como UBER_RADAR`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Radar", "Explora el mapa", "Total $95", "5 km", "15 min"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertEquals(OfferType.UBER_RADAR, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `reserva se detecta como UBER_RESERVATION`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Viaje reservado", "Recogida programada", "Total $150", "12 km", "30 min"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertEquals(OfferType.UBER_RESERVATION, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `viaje en moto se detecta como UBER_MOTO`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Uber Moto", "Aceptar", "Total $60", "4 km", "12 min"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertEquals(OfferType.UBER_MOTO, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `viaje xl se detecta como UBER_XL`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Uber XL", "6 pasajeros", "Total $180", "15 km", "35 min"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertEquals(OfferType.UBER_XL, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `pantalla sin oferta no produce ParsedOffer con oferta`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Dónde quieres ir?", "Buscar", "Disponible"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertNull(parsed.offer)
        assertEquals(OfferType.GENERIC, parsed.type)
    }

    @Test
    fun `sin parser especializado cae al extractor generico`() {
        val parsed =
            OfferParserOrchestrator(
                PlatformDescriptorRegistry(listOf(PlatformDescriptors.UBER.copy(offerTypes = emptyList()))),
            ).parse(
                texts = listOf("Nueva solicitud", "Total $90", "6 km", "18 min"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertEquals(OfferType.GENERIC, parsed.type)
        assertNotNull(parsed.offer)
        assertEquals(90.0, parsed.offer?.estimatedTotal ?: 0.0, 0.001)
    }

    @Test
    fun `parser especializado que no puede extraer cede al siguiente`() {
        // "Reserva" matchea UBER_RESERVATION pero sin monto no extrae; el texto
        // no tiene otra variante y cae al genérico.
        val parsed =
            orchestrator().parse(
                texts = listOf("Reserva", "Nueva solicitud", "Total $70", "4 km"),
                timestampMillis = 1000L,
                platform = RidePlatform.UBER,
            )

        assertNotNull(parsed.offer)
    }

    @Test
    fun `plataforma distinta a uber usa el extractor generico`() {
        val parsed =
            OfferParserOrchestrator(
                PlatformDescriptorRegistry(listOf(PlatformDescriptors.UBER, PlatformDescriptors.DIDI)),
            ).parse(
                texts = listOf("Nueva solicitud", "Total $120", "8.5 km", "25 min"),
                timestampMillis = 1000L,
                platform = RidePlatform.DIDI,
            )

        assertEquals(OfferType.GENERIC, parsed.type)
        assertNotNull(parsed.offer)
        assertTrue(parsed.offer!!.rawText.isNotEmpty())
    }
}
