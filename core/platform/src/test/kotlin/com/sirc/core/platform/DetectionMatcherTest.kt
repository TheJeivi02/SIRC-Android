package com.sirc.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionMatcherTest {
    @Test
    fun `matchesPackage normaliza y compara exacto`() {
        assertTrue(DetectionMatcher.matchesPackage(listOf("com.ubercab"), "  COM.UberCab "))
    }

    @Test
    fun `matchesPackage respeta los aliases del descriptor`() {
        assertTrue(DetectionMatcher.matchesPackage(listOf("com.ubercab", "com.uber"), "com.uber"))
    }

    @Test
    fun `matchesPackage no coincide con paquete distinto`() {
        assertFalse(DetectionMatcher.matchesPackage(listOf("com.ubercab"), "com.cabify.rider"))
    }

    @Test
    fun `matchScore cuenta keywords de deteccion presentes sin duplicados`() {
        val descriptor = PlatformDescriptors.UBER
        val normalized = listOf("aceptar", "rechazar", "nueva solicitud")

        val score = DetectionMatcher.matchScore(descriptor, normalized)

        assertEquals(3, score)
    }

    @Test
    fun `matchScore no cuenta keywords ausentes`() {
        val descriptor = PlatformDescriptors.UBER

        assertEquals(0, DetectionMatcher.matchScore(descriptor, listOf("hola mundo")))
    }

    @Test
    fun `matchScore con textos vacios es cero`() {
        assertEquals(0, DetectionMatcher.matchScore(PlatformDescriptors.UBER, emptyList()))
    }
}
