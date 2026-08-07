package com.sirc.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferDetectionEngineTest {
    private val engine = OfferDetectionEngine()

    @Test
    fun `solicitud entrante se detecta como REQUEST`() {
        val texts =
            listOf(
                "Nueva solicitud de viaje",
                "Aceptar",
                "Rechazar",
                "Ganancia estimada $180 MXN",
                "Recoge a Juan",
            )

        val detection = engine.detect(texts)

        assertEquals(ScreenType.REQUEST, detection.type)
        assertTrue(detection.isRequest)
    }

    @Test
    fun `texto vacio se clasifica como UNKNOWN`() {
        val detection = engine.detect(emptyList())

        assertEquals(ScreenType.UNKNOWN, detection.type)
    }

    @Test
    fun `texto sin patrones se clasifica como UNKNOWN`() {
        val detection = engine.detect(listOf("12345", "QWERTY", "Menu"))

        assertEquals(ScreenType.UNKNOWN, detection.type)
    }

    @Test
    fun `pantalla de inicio se detecta como HOME`() {
        val texts = listOf("Dónde quieres ir?", "Buscar", "Disponible", "Promociones")

        val detection = engine.detect(texts)

        assertEquals(ScreenType.HOME, detection.type)
    }

    @Test
    fun `pantalla de error se detecta como ERROR`() {
        val texts = listOf("Algo salió mal", "Inténtalo de nuevo")

        val detection = engine.detect(texts)

        assertEquals(ScreenType.ERROR, detection.type)
    }

    @Test
    fun `pantalla desconectada se detecta como OFFLINE`() {
        val texts = listOf("Estás desconectado", "Toca para conectarte")

        val detection = engine.detect(texts)

        assertEquals(ScreenType.OFFLINE, detection.type)
    }

    @Test
    fun `pantalla de navegacion se detecta como NAVIGATION`() {
        val texts = listOf("Gira a la derecha", "Llegada en 500 m")

        val detection = engine.detect(texts)

        assertEquals(ScreenType.NAVIGATION, detection.type)
    }

    @Test
    fun `viaje en curso se detecta como TRIP`() {
        val texts = listOf("Viaje en curso", "Finalizar", "Recogida completada")

        val detection = engine.detect(texts)

        assertEquals(ScreenType.TRIP, detection.type)
    }

    @Test
    fun `acentos y mayusculas no impiden la deteccion`() {
        val texts = listOf("NUEVA SOLICITUD DE VIAJE", "GANANCIA ESTIMADA $200")

        val detection = engine.detect(texts)

        assertEquals(ScreenType.REQUEST, detection.type)
    }

    @Test
    fun `solicitud tiene prioridad sobre palabras sueltas de otras pantallas`() {
        val texts = listOf("Nueva solicitud", "Aceptar", "En línea")

        val detection = engine.detect(texts)

        assertEquals(ScreenType.REQUEST, detection.type)
    }

    @Test
    fun `normalize quita acentos y pasa a minusculas`() {
        assertEquals("nueva solicitud de viaje", OfferDetectionEngine.normalize("Nueva Solicitud de VíaJe"))
    }

    @Test
    fun `keywordsFor devuelve las palabras clave de una pantalla`() {
        val keywords = engine.keywordsFor(ScreenType.REQUEST)

        assertTrue(keywords.contains("aceptar"))
        assertFalse(keywords.isEmpty())
    }
}
