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
    fun `matchScore cuenta identificadores fuertes de plataforma presentes`() {
        val descriptor = PlatformDescriptors.UBER
        val normalized = listOf("uber", "aceptar")

        val score = DetectionMatcher.matchScore(descriptor, normalized)

        assertEquals(1, score)
    }

    @Test
    fun `matchScore no duplica identificadores repetidos`() {
        val descriptor = PlatformDescriptors.UBER
        val normalized = listOf("UBER", "uber", "UberX")

        assertEquals(1, DetectionMatcher.matchScore(descriptor, normalized))
    }

    @Test
    fun `matchScore no puntua palabras genericas de pantalla como identificador`() {
        val descriptor = PlatformDescriptors.UBER
        val normalized = listOf("aceptar", "rechazar", "nueva solicitud", "viaje", "oferta", "tarifa")

        assertEquals(0, DetectionMatcher.matchScore(descriptor, normalized))
    }

    @Test
    fun `matchScore no puntua una plataforma con identificadores de otra`() {
        val descriptor = PlatformDescriptors.UBER
        val normalized = listOf("indriver", "didi", "cabify")

        assertEquals(0, DetectionMatcher.matchScore(descriptor, normalized))
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
