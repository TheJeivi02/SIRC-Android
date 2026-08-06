package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlatformDescriptorRegistryTest {
    // --- Lookups ---

    @Test
    fun `resuelve descriptor por plataforma y por paquete`() {
        val registry = PlatformDescriptorRegistry(listOf(validDescriptor()))

        assertEquals(RidePlatform.UBER, registry.descriptorFor(RidePlatform.UBER)?.platform)
        assertEquals(RidePlatform.UBER, registry.descriptorForPackageName("com.ubercab")?.platform)
    }

    @Test
    fun `resuelve motor de deteccion y extractor como instancias unicas`() {
        val registry = PlatformDescriptorRegistry(listOf(validDescriptor()))

        assertSame(registry.detectionEngineFor(RidePlatform.UBER), registry.detectionEngineFor(RidePlatform.UBER))
        assertSame(registry.extractorFor(RidePlatform.UBER), registry.extractorFor(RidePlatform.UBER))
    }

    @Test
    fun `exponen los parsers de variantes del descriptor en orden`() {
        val registry = PlatformDescriptorRegistry(listOf(validDescriptor()))

        val parsers = registry.variantParsersFor(RidePlatform.UBER)
        assertEquals(1, parsers.size)
        assertEquals(OfferType.UBER_REQUEST, parsers.first().type)
    }

    @Test
    fun `plataforma no registrada devuelve none y sin parsers`() {
        val registry = PlatformDescriptorRegistry(listOf(validDescriptor()))

        assertNull(registry.descriptorFor(RidePlatform.CABIFY))
        assertNull(registry.descriptorForPackageName("com.cabify.rider"))
        assertNull(registry.detectionEngineFor(RidePlatform.CABIFY))
        assertNull(registry.extractorFor(RidePlatform.CABIFY))
        assertTrue(registry.variantParsersFor(RidePlatform.CABIFY).isEmpty())
    }

    // --- Validación: falla rápido en construcción ---

    @Test
    fun `rechaza plataforma duplicada`() {
        val a = validDescriptor(RidePlatform.UBER, packageNames = listOf("com.a"))
        val b = validDescriptor(RidePlatform.UBER, packageNames = listOf("com.b"))

        assertInvalid { PlatformDescriptorRegistry(listOf(a, b)) }
    }

    @Test
    fun `rechaza alias de paquete duplicado`() {
        val a = validDescriptor(RidePlatform.UBER, packageNames = listOf("com.x"))
        val b = validDescriptor(RidePlatform.DIDI, packageNames = listOf("com.x"))

        assertInvalid { PlatformDescriptorRegistry(listOf(a, b)) }
    }

    @Test
    fun `rechaza descriptor sin reglas de deteccion`() {
        assertInvalid { PlatformDescriptorRegistry(listOf(validDescriptor(detectionRules = emptyList()))) }
    }

    @Test
    fun `rechaza regla de deteccion sin keywords`() {
        val rules = listOf(DetectionRule(ScreenType.REQUEST, 3f, emptyList()))

        assertInvalid { PlatformDescriptorRegistry(listOf(validDescriptor(detectionRules = rules))) }
    }

    @Test
    fun `rechaza variante de oferta sin keywords`() {
        val offerTypes = listOf(OfferTypeVariant(OfferType.UBER_REQUEST, emptyList()))

        assertInvalid { PlatformDescriptorRegistry(listOf(validDescriptor(offerTypes = offerTypes))) }
    }

    @Test
    fun `rechaza extractor sin keywords`() {
        val keywords = PlatformKeywords(emptyList(), emptyList())

        assertInvalid { PlatformDescriptorRegistry(listOf(validDescriptor(extractorKeywords = keywords))) }
    }

    @Test
    fun `rechaza moneda invalida`() {
        assertInvalid { PlatformDescriptorRegistry(listOf(validDescriptor(defaultCurrency = "mxn"))) }
    }

    @Test
    fun `rechaza descriptor sin regla REQUEST`() {
        val rules = listOf(DetectionRule(ScreenType.HOME, 1f, listOf("buscar")))

        assertInvalid { PlatformDescriptorRegistry(listOf(validDescriptor(detectionRules = rules))) }
    }

    @Test
    fun `rechaza variantes de oferta duplicadas`() {
        val offerTypes =
            listOf(
                OfferTypeVariant(OfferType.UBER_REQUEST, listOf("aceptar")),
                OfferTypeVariant(OfferType.UBER_REQUEST, listOf("rechazar")),
            )

        assertInvalid { PlatformDescriptorRegistry(listOf(validDescriptor(offerTypes = offerTypes))) }
    }

    // --- Helpers ---

    private fun validDescriptor(
        platform: RidePlatform = RidePlatform.UBER,
        packageNames: List<String> = listOf(platform.packageName),
        detectionRules: List<DetectionRule> = OfferDetectionEngine.defaultRules(),
        offerTypes: List<OfferTypeVariant> =
            listOf(OfferTypeVariant(OfferType.UBER_REQUEST, listOf("nueva solicitud", "aceptar"))),
        extractorKeywords: PlatformKeywords = PlatformKeywords(listOf("total", "recibe"), listOf("tarifa")),
        defaultCurrency: String = "MXN",
    ): PlatformDescriptor =
        PlatformDescriptor(
            platform = platform,
            packageNames = packageNames,
            detectionRules = detectionRules,
            offerTypes = offerTypes,
            extractorKeywords = extractorKeywords,
            defaultCurrency = defaultCurrency,
        )

    private fun assertInvalid(block: () -> Unit) {
        try {
            block()
            fail("se esperaba una IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // esperado: fallo rápido en construcción
        }
    }
}
