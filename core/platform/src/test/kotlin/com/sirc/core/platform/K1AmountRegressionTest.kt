package com.sirc.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regresión K1 — selección de monto con los textos OCR REALES del dataset de
 * DEVICE-01 (dump `docs/testing/evidence/SIRC_OCR_TEST_logcat_dump.txt`).
 *
 * Antes del fix: indriver_1 → 479.0, indriver_2 → 5.0, uber_2 → 90.0.
 * Después: 4.5 / 4.5 / 25.53 (montos correctos del snapshot real).
 */
class K1AmountRegressionTest {
    private val parser = OfferTextParser()

    private fun parseWithDetection(
        texts: List<String>,
        packageName: String,
        descriptors: List<PlatformDescriptor> =
            listOf(PlatformDescriptors.UBER, PlatformDescriptors.INDRIVE),
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
        )
    }

    // ---------- Parser: ruido de dirección / ceros a la izquierda ----------

    @Test
    fun `parser ignora numeros de direccion sin moneda ni contexto`() {
        val parsed = parser.parse(listOf("479 A 1 Pa. 24B NO"))
        assertTrue("no debe producir candidatos de monto: ${parsed.amounts}", parsed.amounts.isEmpty())
    }

    @Test
    fun `parser ignora montos con cero a la izquierda como $090`() {
        val parsed = parser.parse(listOf("5.00 (9) ) $090 incluido"))
        assertTrue("no debe producir candidatos de monto: ${parsed.amounts}", parsed.amounts.isEmpty())
    }

    @Test
    fun `parser ignora rating y conteo de reseñas`() {
        val parsed = parser.parse(listOf("4.7 (21) $4.50, Efectivo"))
        assertEquals(1, parsed.amounts.size)
        assertEquals(4.5, parsed.amounts.first().value, 0.001)
    }

    @Test
    fun `parser mantiene monto latino con simbolo y contexto aceptar`() {
        val parsed = parser.parse(listOf("Aceptar por $4,50"))
        assertEquals(1, parsed.amounts.size)
        assertEquals(4.5, parsed.amounts.first().value, 0.001)
        assertTrue(parsed.amounts.first().hasCurrencyMarker)
    }

    @Test
    fun `parser admite monto USD con sufijo y contexto aceptar`() {
        val parsed = parser.parse(listOf("Aceptar por USD4.5"))
        assertEquals(1, parsed.amounts.size)
        assertEquals(4.5, parsed.amounts.first().value, 0.001)
        assertEquals("USD", parsed.amounts.first().currency)
    }

    // ---------- Fixtures E2E reales (oferta → parsing) ----------

    @Test
    fun `indriver_1 extrae monto real 4_50 en vez del numero de calle 479`() {
        val texts =
            listOf(
                "11:24 D",
                "Gocglb",
                "Cami",
                "Solicitud de viaje",
                "16,4 km",
                "B",
                "-5,5 km",
                "$4,50",
                "os Loias",
                "$5",
                "13 min.",
                "5,5 km",
                "MA Samborondón",
                "479 A 1 Pa. 24B NO",
                "juto ahora B La Mansión Night Club. (Vía a",
                "Daule, Guayaquil)",
                "Aceptar por $4,50",
                "Ofrece tu tarifa",
                "$5,40",
                "Cerrar",
                "$5,90",
                "Casigua",
            )
        val parsed = parseWithDetection(texts, packageName = "com.leadingsoft.ride.driver")

        assertNotNull(parsed.offer)
        assertEquals(4.5, parsed.offer!!.estimatedTotal ?: 0.0, 0.001)
        assertEquals(16.4, parsed.offer!!.distanceKm ?: 0.0, 0.001)
        assertEquals(13.0, parsed.offer!!.durationMin ?: 0.0, 0.001)
    }

    @Test
    fun `indriver_2 extrae monto real 4_5 en vez de 5_0`() {
        val texts =
            listOf(
                "9:31 p. m",
                "Cerro Guagua",
                "Pichincha",
                "Google",
                "Maria",
                "Chingui",
                "45 seg.",
                "Parrogin 42 min",
                "28,7 kn",
                "Nono",
                "Lloa",
                "Qúitoumbisi",
                "1 min.",
                "San375 metro",
                "S550 8",
                "• N70E 7030",
                "4.7 (21) $4.50, Efectivo",
                "USD5",
                "Solicitud de negocio",
                "Entregas Puerta a puerta",
                "Funda pequeña",
                "Sangolqu",
                "Aceptar por USD4.5",
                "ofrece tu tarifa",
                "USD5.4",
                "Ignorar oferta",
                "P +",
                "-375 metro",
                "Pintan",
                "US >",
            )
        val parsed = parseWithDetection(texts, packageName = "com.leadingsoft.ride.driver")

        assertNotNull(parsed.offer)
        assertEquals(4.5, parsed.offer!!.estimatedTotal ?: 0.0, 0.001)
        assertEquals(42.0, parsed.offer!!.durationMin ?: 0.0, 0.001)
    }

    @Test
    fun `uber_2 extrae monto real 25_53 en vez de 90_0`() {
        val texts =
            listOf(
                "Suiza",
                "1 min",
                "Embotellamiento",
                "los Shyris",
                "ista de patinaje",
                "'arque La Carolina",
                "Parque La",
                "Pablo Arturo Suárez",
                "A uberX Exclusivo",
                "$25.53",
                "A4 min (0.3 km)",
                "Identidad digital verificada o TC",
                "Noruega, Iñaquito",
                "Juan Severino",
                "Francisco de Andrade Marin",
                "5.00 (9) ) $090 incluido",
                "Carlos Tobar",
                "Aceptar",
                "Viaje: 2 h 8 min (99.0 km)",
                "La Peña",
                "A Viaje largo (45+ min)",
            )
        val parsed = parseWithDetection(texts, packageName = "com.ubercab")

        assertNotNull(parsed.offer)
        assertEquals(25.53, parsed.offer!!.estimatedTotal ?: 0.0, 0.001)
        assertEquals(99.0, parsed.offer!!.distanceKm ?: 0.0, 0.001)
        assertEquals(128.0, parsed.offer!!.durationMin ?: 0.0, 0.001)
    }

    // ---------- Fixtures E2E reales (pantallas sin oferta) ----------

    @Test
    fun `uber_1 pantalla sin oferta no produce oferta`() {
        val texts =
            listOf(
                "coLLAO", "NC", "CARCELÉN", "ouITO", "A UberX", "$6.90", "CALDERON",
                "4.80 (58)", "Identidad digital verificada o TC", "A6 min (2.6 km)",
                "Puembo", "Av. de la República, Rumipamba", "Viaje: 32 min (31.9 km)",
                "Via El Quinche - Guayllabamba 79,", "Guayllabamba", "X", "LABAM",
            )
        val parsed = parseWithDetection(texts, packageName = "com.ubercab")
        assertNull(parsed.offer)
    }

    @Test
    fun `uber_3 pantalla home sin oferta no produce oferta`() {
        val texts =
            listOf(
                "11:07 PM GO", "Antena", "ta a Rucu", "Google", "Cc EL Bosque", "|2 uberX",
                "Voto Nacional", "$3.66", "* 4.99 (105)", "A4 min (1.8 km)", "Canada, San Juan",
                "PAMBA", "Basilica deSuRORESTA", "CARCELE", "Condato shonnino",
                "Viaje: 19 min (16.3 km)", "Condado", "IRAQUTO", "coNTE", "UEL PUEDLC",
                "ACARCLUINA", "D ldentidad digital verificadao TC", "GUAPULO",
                "OE7 N79-200 Y, Rancho Bajo, El", "Viaje disponible", "ANO GRA", "X", "MEAY─",
            )
        val parsed = parseWithDetection(texts, packageName = "com.ubercab")
        assertNull(parsed.offer)
    }
}
