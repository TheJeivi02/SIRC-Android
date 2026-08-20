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
        platformKeywords: List<String> = emptyList(),
    ): PlatformDescriptor =
        PlatformDescriptor(
            platform = platform,
            packageNames = packageNames,
            platformKeywords = platformKeywords,
            detectionRules =
                listOf(
                    DetectionRule(ScreenType.REQUEST, 3f, requestKeywords),
                    DetectionRule(ScreenType.HOME, 1f, listOf("buscar")),
                ),
            offerTypes = emptyList(),
            extractorKeywords = PlatformKeywords(listOf("total"), listOf("tarifa")),
            defaultCurrency = "MXN",
        )

    private val uber = descriptor(RidePlatform.UBER, listOf("aceptar", "rechazar"), platformKeywords = listOf("uber"))
    private val didi =
        descriptor(
            RidePlatform.DIDI,
            listOf("aceptar didi", "solicitud didi"),
            platformKeywords = listOf("didi"),
        )

    private fun engine(vararg descriptors: PlatformDescriptor) =
        PlatformDetectionEngine(PlatformDescriptorRegistry(descriptors.toList()))

    @Test
    fun `packageName con match unico resuelve PACKAGE_MATCH`() {
        val result =
            engine(uber, didi).detect(
                texts = listOf("Aceptar"),
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
                packageName = " COM.UberCab ",
            )

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
    }

    @Test
    fun `sin packageName y un unico candidato por keywords resuelve KEYWORD_CANDIDATE`() {
        val result = engine(uber, didi).detect(texts = listOf("Aceptar", "Rechazar", "Uber"))

        assertEquals(DetectionResolution.KEYWORD_CANDIDATE, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
        assertEquals(1, result.candidates.size)
    }

    @Test
    fun `empate de candidatos por keywords resuelve AMBIGUOUS sin elegir`() {
        val sameA = descriptor(RidePlatform.UBER, listOf("aceptar", "rechazar"))
        val sameB = descriptor(RidePlatform.DIDI, listOf("aceptar", "rechazar"))
        val result = engine(sameA, sameB).detect(texts = listOf("Aceptar", "Rechazar"))

        assertEquals(DetectionResolution.AMBIGUOUS, result.resolution)
        assertNull(result.descriptor)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun `sin candidatos resuelve NONE`() {
        val result = engine(uber, didi).detect(texts = listOf("Hola mundo"))

        assertEquals(DetectionResolution.NONE, result.resolution)
        assertNull(result.descriptor)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun `textos vacios resuelven NONE`() {
        val result = engine(uber).detect(texts = emptyList())

        assertEquals(DetectionResolution.NONE, result.resolution)
    }

    @Test
    fun `propaga el origin recibido`() {
        val result =
            engine(uber, didi).detect(
                texts = listOf("Aceptar"),
                packageName = "com.ubercab",
                origin = CaptureInputType.OCR,
            )

        assertEquals(CaptureInputType.OCR, result.origin)
    }

    @Test
    fun `el seed resuelve InDrive con el paquete de Ecuador`() {
        val registry = PlatformDescriptorRegistry(PlatformDescriptors.all())

        val result =
            PlatformDetectionEngine(registry).detect(
                texts = emptyList(),
                packageName = "sinet.startup.inDriver",
            )

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.INDRIVE, result.descriptor?.platform)
    }

    @Test
    fun `el seed resuelve Uber con el paquete de la app de conductor`() {
        val registry = PlatformDescriptorRegistry(PlatformDescriptors.all())

        val result =
            PlatformDetectionEngine(registry).detect(
                texts = emptyList(),
                packageName = "com.ubercab.driver",
            )

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
    }

    // ---- Matriz de regresión G2 (detección multi-plataforma determinista) ----

    private val seedEngine = PlatformDetectionEngine(PlatformDescriptorRegistry(PlatformDescriptors.all()))

    @Test
    fun `package uber mas texto ambiguo resuelve UBER`() {
        val result = seedEngine.detect(texts = listOf("aceptar", "viaje"), packageName = "com.ubercab")

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
    }

    @Test
    fun `package uber con texto de otra plataforma gana el package`() {
        val result = seedEngine.detect(texts = listOf("didi", "aceptar"), packageName = "com.ubercab")

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
    }

    @Test
    fun `didi inequivoco por package resuelve DIDI`() {
        val result = seedEngine.detect(texts = listOf("aceptar", "didi"), packageName = "com.didiglobal.passenger")

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.DIDI, result.descriptor?.platform)
    }

    @Test
    fun `cabify inequivoco por package resuelve CABIFY`() {
        val result = seedEngine.detect(texts = listOf("aceptar", "cabify"), packageName = "com.cabify.rider")

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.CABIFY, result.descriptor?.platform)
    }

    @Test
    fun `indrive inequivoco por package resuelve INDRIVE`() {
        val result =
            seedEngine.detect(
                texts = listOf("aceptar", "inDriver"),
                packageName = "com.leadingsoft.ride.driver",
            )

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.INDRIVE, result.descriptor?.platform)
    }

    @Test
    fun `sin package keyword uber inequivoca resuelve UBER`() {
        val result = seedEngine.detect(texts = listOf("Nueva solicitud", "Aceptar", "UberX"))

        assertEquals(DetectionResolution.KEYWORD_CANDIDATE, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
    }

    @Test
    fun `sin package keyword didi inequivoca resuelve DIDI`() {
        val result = seedEngine.detect(texts = listOf("Solicitud de viaje", "Aceptar", "DiDi"))

        assertEquals(DetectionResolution.KEYWORD_CANDIDATE, result.resolution)
        assertEquals(RidePlatform.DIDI, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
    }

    @Test
    fun `sin package keyword cabify inequivoca resuelve CABIFY`() {
        val result = seedEngine.detect(texts = listOf("Solicitud", "Aceptar", "Cabify"))

        assertEquals(DetectionResolution.KEYWORD_CANDIDATE, result.resolution)
        assertEquals(RidePlatform.CABIFY, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
    }

    @Test
    fun `sin package keyword indriver inequivoca resuelve INDRIVE`() {
        val result = seedEngine.detect(texts = listOf("Solicitud de viaje", "Aceptar", "inDriver"))

        assertEquals(DetectionResolution.KEYWORD_CANDIDATE, result.resolution)
        assertEquals(RidePlatform.INDRIVE, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
    }

    @Test
    fun `palabras genericas sin marca resuelven AMBIGUOUS`() {
        val result = seedEngine.detect(texts = listOf("Solicitud de viaje", "Aceptar", "Rechazar", "Tarifa", "Oferta"))

        assertEquals(DetectionResolution.AMBIGUOUS, result.resolution)
        assertNull(result.descriptor)
        assertEquals(4, result.candidates.size)
    }

    @Test
    fun `dos marcas presentes resuelven AMBIGUOUS`() {
        val result = seedEngine.detect(texts = listOf("Aceptar", "Uber", "DiDi"))

        assertEquals(DetectionResolution.AMBIGUOUS, result.resolution)
        assertNull(result.descriptor)
    }

    @Test
    fun `texto vacio con package resuelve la plataforma del package`() {
        val result = seedEngine.detect(texts = emptyList(), packageName = "com.ubercab")

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
    }

    @Test
    fun `sin package ni marca ni pantalla reconocida resuelve NONE`() {
        val result = seedEngine.detect(texts = listOf("Hola mundo"))

        assertEquals(DetectionResolution.NONE, result.resolution)
        assertNull(result.descriptor)
    }

    @Test
    fun `solo marca sin contexto de pantalla resuelve NONE (conservador)`() {
        val result = seedEngine.detect(texts = listOf("Uber"))

        assertEquals(DetectionResolution.NONE, result.resolution)
        assertNull(result.descriptor)
    }
}
