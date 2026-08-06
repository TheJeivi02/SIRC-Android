package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformDetectionEngineTest {
    private fun descriptor(
        platform: RidePlatform,
        requestKeywords: List<String>,
        packageNames: List<String> = listOf(platform.packageName),
    ): PlatformDescriptor =
        PlatformDescriptor(
            platform = platform,
            packageNames = packageNames,
            detectionRules =
                listOf(
                    DetectionRule(ScreenType.REQUEST, 3f, requestKeywords),
                    DetectionRule(ScreenType.HOME, 1f, listOf("buscar")),
                ),
            offerTypes = emptyList(),
            extractorKeywords = PlatformKeywords(listOf("total"), listOf("tarifa")),
            defaultCurrency = "MXN",
        )

    private val uber = descriptor(RidePlatform.UBER, listOf("aceptar", "rechazar"))
    private val didi = descriptor(RidePlatform.DIDI, listOf("aceptar didi", "solicitud didi"))

    private fun engine(vararg descriptors: PlatformDescriptor) =
        PlatformDetectionEngine(PlatformDescriptorRegistry(descriptors.toList()))

    @Test
    fun `packageName con match unico resuelve PACKAGE_MATCH`() {
        val result =
            engine(uber, didi).detect(
                texts = listOf("Aceptar"),
                timestampMillis = 1000L,
                packageName = "com.ubercab",
            )

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
        assertEquals("com.ubercab", result.sourcePackage)
    }

    @Test
    fun `packageName normalizado con alias resuelve PACKAGE_MATCH`() {
        val result =
            engine(uber, didi).detect(
                texts = listOf("Aceptar"),
                timestampMillis = 1000L,
                packageName = " COM.UberCab ",
            )

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
    }

    @Test
    fun `sin packageName y un unico candidato por keywords resuelve KEYWORD_CANDIDATE`() {
        val result = engine(uber, didi).detect(texts = listOf("Aceptar", "Rechazar"), timestampMillis = 1000L)

        assertEquals(DetectionResolution.KEYWORD_CANDIDATE, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
        assertEquals(1, result.candidates.size)
    }

    @Test
    fun `empate de candidatos por keywords resuelve AMBIGUOUS sin elegir`() {
        val sameA = descriptor(RidePlatform.UBER, listOf("aceptar", "rechazar"))
        val sameB = descriptor(RidePlatform.DIDI, listOf("aceptar", "rechazar"))
        val result = engine(sameA, sameB).detect(texts = listOf("Aceptar", "Rechazar"), timestampMillis = 1000L)

        assertEquals(DetectionResolution.AMBIGUOUS, result.resolution)
        assertNull(result.descriptor)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun `sin candidatos resuelve NONE`() {
        val result = engine(uber, didi).detect(texts = listOf("Hola mundo"), timestampMillis = 1000L)

        assertEquals(DetectionResolution.NONE, result.resolution)
        assertNull(result.descriptor)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun `textos vacios resuelven NONE`() {
        val result = engine(uber).detect(texts = emptyList(), timestampMillis = 1000L)

        assertEquals(DetectionResolution.NONE, result.resolution)
    }

    @Test
    fun `propaga el origin recibido`() {
        val result =
            engine(uber, didi).detect(
                texts = listOf("Aceptar"),
                timestampMillis = 1000L,
                packageName = "com.ubercab",
                origin = DetectionOrigin.OCR,
            )

        assertEquals(DetectionOrigin.OCR, result.origin)
    }
}
