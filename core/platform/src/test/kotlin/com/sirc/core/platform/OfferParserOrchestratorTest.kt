package com.sirc.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferParserOrchestratorTest {
    private fun parse(
        texts: List<String>,
        packageName: String = PACKAGE_UBER,
        descriptors: List<PlatformDescriptor> = listOf(PlatformDescriptors.UBER),
        detectionMillis: Double = 0.0,
    ): ParsedOffer {
        val registry = PlatformDescriptorRegistry(descriptors)
        val result =
            PlatformDetectionEngine(registry).detect(
                texts = texts,
                packageName = packageName,
            )
        return OfferParserOrchestrator(registry).parse(
            result = result,
            texts = texts,
            timestampMillis = 1000L,
            detectionMillis = detectionMillis,
        )
    }

    @Test
    fun `solicitud estandar se detecta como UBER_REQUEST`() {
        val parsed =
            parse(listOf("Nueva solicitud de viaje", "Aceptar", "Rechazar", "Total $120", "8.5 km", "25 min"))

        assertEquals(OfferType.UBER_REQUEST, parsed.type)
        assertNotNull(parsed.offer)
        assertEquals(120.0, parsed.offer?.estimatedTotal ?: 0.0, 0.001)
    }

    @Test
    fun `radar se detecta como UBER_RADAR`() {
        val parsed = parse(listOf("Radar", "Explora el mapa", "Total $95", "5 km", "15 min"))

        assertEquals(OfferType.UBER_RADAR, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `reserva se detecta como UBER_RESERVATION`() {
        val parsed = parse(listOf("Viaje reservado", "Recogida programada", "Total $150", "12 km", "30 min"))

        assertEquals(OfferType.UBER_RESERVATION, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `viaje en moto se detecta como UBER_MOTO`() {
        val parsed = parse(listOf("Uber Moto", "Aceptar", "Total $60", "4 km", "12 min"))

        assertEquals(OfferType.UBER_MOTO, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `viaje xl se detecta como UBER_XL`() {
        val parsed = parse(listOf("Uber XL", "6 pasajeros", "Total $180", "15 km", "35 min"))

        assertEquals(OfferType.UBER_XL, parsed.type)
        assertNotNull(parsed.offer)
    }

    @Test
    fun `pantalla sin oferta no produce ParsedOffer con oferta`() {
        val parsed = parse(listOf("Dónde quieres ir?", "Buscar", "Disponible"))

        assertNull(parsed.offer)
        assertEquals(OfferType.GENERIC, parsed.type)
    }

    @Test
    fun `sin parser especializado cae al extractor generico`() {
        val parsed =
            parse(
                texts = listOf("Nueva solicitud", "Total $90", "6 km", "18 min"),
                descriptors = listOf(PlatformDescriptors.UBER.copy(offerTypes = emptyList())),
            )

        assertEquals(OfferType.GENERIC, parsed.type)
        assertNotNull(parsed.offer)
        assertEquals(90.0, parsed.offer?.estimatedTotal ?: 0.0, 0.001)
    }

    @Test
    fun `parser especializado que no puede extraer cede al siguiente`() {
        // "Reserva" matchea UBER_RESERVATION pero sin monto no extrae; el texto
        // no tiene otra variante y cae al genérico.
        val parsed = parse(listOf("Reserva", "Nueva solicitud", "Total $70", "4 km"))

        assertNotNull(parsed.offer)
    }

    @Test
    fun `plataforma distinta a uber usa el extractor generico`() {
        val parsed =
            parse(
                texts = listOf("Nueva solicitud", "Total $120", "8.5 km", "25 min"),
                packageName = PACKAGE_DIDI,
                descriptors = listOf(PlatformDescriptors.UBER, PlatformDescriptors.DIDI),
            )

        assertEquals(OfferType.GENERIC, parsed.type)
        assertNotNull(parsed.offer)
        assertTrue(parsed.offer!!.rawText.isNotEmpty())
    }

    @Test
    fun `package de plataforma no registrada devuelve none`() {
        val parsed =
            parse(
                texts = listOf("Texto irrelevante sin keywords"),
                packageName = "com.desconocido.app",
            )

        assertNull(parsed.offer)
    }

    @Test
    fun `pantalla no request devuelve none`() {
        val parsed = parse(listOf("Dónde quieres ir?", "Buscar", "Disponible"))

        assertNull(parsed.offer)
    }

    @Test
    fun `parse por result reconocido propaga el detectionMillis`() {
        val texts = listOf("Nueva solicitud de viaje", "Aceptar", "Rechazar", "Total $120", "8.5 km", "25 min")
        val registry = PlatformDescriptorRegistry(listOf(PlatformDescriptors.UBER))
        val result =
            PlatformDetectionEngine(registry).detect(
                texts = texts,
                packageName = PACKAGE_UBER,
                origin = CaptureInputType.OCR,
            )

        val parsed =
            OfferParserOrchestrator(registry).parse(
                result = result,
                texts = texts,
                timestampMillis = 1000L,
                detectionMillis = 50.0,
            )

        assertEquals(OfferType.UBER_REQUEST, parsed.type)
        assertNotNull(parsed.offer)
        assertEquals(120.0, parsed.offer?.estimatedTotal ?: 0.0, 0.001)
        assertEquals(50.0, parsed.detectionMillis, 0.001)
    }

    @Test
    fun `parse por result no reconocido devuelve none`() {
        val texts = listOf("Dónde quieres ir?", "Buscar", "Disponible")
        val registry = PlatformDescriptorRegistry(listOf(PlatformDescriptors.UBER))
        val result =
            PlatformDetectionEngine(registry).detect(
                texts = texts,
                packageName = PACKAGE_UBER,
                origin = CaptureInputType.OCR,
            )

        val parsed =
            OfferParserOrchestrator(registry).parse(
                result = result,
                texts = texts,
                timestampMillis = 1000L,
            )

        assertNull(parsed.offer)
    }

    @Test
    fun `parse por result sin descriptor devuelve none`() {
        val texts = listOf("Dónde quieres ir?", "Buscar", "Disponible")
        val registry = PlatformDescriptorRegistry(listOf(PlatformDescriptors.UBER))
        val result =
            PlatformDetectionEngine(registry).detect(
                texts = texts,
                packageName = PACKAGE_UBER,
                origin = CaptureInputType.OCR,
            )

        val parsed =
            OfferParserOrchestrator(registry).parse(
                result = result,
                texts = texts,
                timestampMillis = 1000L,
            )

        assertNull(parsed.offer)
    }

    private companion object {
        const val PACKAGE_UBER = "com.ubercab"
        const val PACKAGE_DIDI = "com.didiglobal.passenger"
    }
}
