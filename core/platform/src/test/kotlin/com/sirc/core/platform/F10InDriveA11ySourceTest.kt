package com.sirc.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FASE 10 — reproducción del defecto `distanceKm=0.0` en el 100 % de las 141
 * ofertas reales de InDrive (20-ago-2026).
 *
 * Entrada: textos EXACTOS del árbol de accesibilidad capturado en vivo
 * (uiautomator dump `corr2_173051.xml`, correlacionado con
 * `overlay mostrando: INDRIVE · $3.7`, pantalla de descubrimiento de ofertas
 * de InDrive, package `sinet.startup.inDriver`). Es la misma fuente que
 * consume `AccessibilityCaptureInput.collectTexts`.
 *
 * Evidencia del dump: monto y duración presentes en la jerarquía
 * ("Tarifa recomendada $3.70", "…~18\u00A0min."), distancia AUSENTE por
 * completo (0 nodos con km/km/metro en 35 dumps correlacionados: la distancia
 * solo se dibuja en la superficie del mapa de Google, no accesible).
 *
 * El defecto de distancia NO es del parser (veáse `parser_extrae` y
 * `K1AmountRegressionTest.indriver_1` con "16,4 km" → 16.4): es la FUENTE
 * (a11y) la que no entrega distancia en este flujo → `distanceKm = null`
 * (y `?: 0.0` en PlatformOfferParser lo persiste como 0.0).
 *
 * NOTA duración: el dump serializado trae NBSP (U+00A0) en "~18\u00A0min.",
 * que `\s` de Kotlin/Java no matchea. Con la corrección FASE 10-D el parser
 * normaliza NBSP→espacio y extrae 18.0; sin ella, la duración se perdería y
 * quedaría el "2 min" de las tarjetas (espera estimada de aceptación, NO
 * duración de viaje). El device en tiempo de evento ve "X min" con espacio
 * normal (DB: duraciones 8-63 min reales, p. ej. 41 min para "~41 min.").
 */
class F10InDriveA11ySourceTest {
    private fun parseWithDetection(
        texts: List<String>,
        packageName: String,
    ): ParsedOffer {
        val registry = PlatformDescriptorRegistry(listOf(PlatformDescriptors.INDRIVE))
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

    private fun realDiscoveryA11yTexts(): List<String> =
        listOf(
            "Aceptar autom\u00E1ticamente la oferta de $3.70",
            "Opciones y comentarios para esta solicitud de viaje",
            "Encontrar ofertas",
            "Encontrar ofertas",
            "M\u00E9todo de pago seleccionado: Banco Pichincha .button",
            "Sin tr\u00E1fico, precios bajos",
            "~$2",
            "1",
            "Moto",
            "Conductores y carros seleccionados",
            "~$4.20",
            "2 min",
            "\u2022",
            "4",
            "Confort",
            "Mejor auto",
            "~$3.90",
            "4",
            "Viaje+",
            "Viajes econ\u00F3micos",
            "2 min",
            "\u2022",
            "4",
            "Viaje",
            "Tarifa recomendada",
            "$3.70",
            "A\u00F1ade una parada durante tu viaje",
            "A Av Diego V\u00E1squez de Cepeda 170120",
            "Av Diego V\u00E1squez de Cepeda 170120 ~18\u00A0min.",
            "Portal",
            "De N84B Oe8 13",
            "N84B Oe8 13",
            "Mapa de Google",
            "sinet.startup.inDriver:id/WVRoGAXK58Pz9QMO_ContractorIcon_prh.c.b_LARGE",
            "sinet.startup.inDriver:id/Jy9QNWYevnKGBArd_ContractorIcon_prh.c.b_LARGE",
            "sinet.startup.inDriver:id/nJvbGOy9vxJNRQEP_ContractorIcon_prh.c.b_LARGE",
            "sinet.startup.inDriver:id/nJvbGOx6l61mRQEP_ContractorIcon_prh.c.b_LARGE",
            "sinet.startup.inDriver:id/YavlzEx6KWBNJ51W_ContractorIcon_prh.c.b_LARGE",
            "sinet.startup.inDriver:id/PKnrNbagVx4m6ega_ContractorIcon_prh.c.b_LARGE",
            "sinet.startup.inDriver:id/kY4aGlqbqywmpBe2_ContractorIcon_prh.c.b_LARGE",
            "sinet.startup.inDriver:id/f4d50c8f-c0c8-480c-9ae4-aea1814787ef_LandingPointIcon",
            "sinet.startup.inDriver:id/68ed61ee-d486-4931-b1d6-3aaebc038da0_LandingPointIcon",
            "sinet.startup.inDriver:id/3e3623ab-19d2-4756-b43f-e48bf8dd07c0_LandingPointIcon",
            "sinet.startup.inDriver:id/e30fb7ed-211a-4d7e-93c5-3f84b5dea5ec_LandingPointIcon",
            "sinet.startup.inDriver:id/0ad20b1c-fbe5-46a9-819c-6edb852c36a3_LandingPointIcon",
            "sinet.startup.inDriver:id/bf62e7ce-9c9c-49d6-9acd-000d75448bf2_LandingPointIcon",
            "sinet.startup.inDriver:id/cfa4bed8-2124-46c3-bdf4-acac3137a1e0_LandingPointIcon",
            "sinet.startup.inDriver:id/3754bd5c-96bb-4b53-a2fa-82b2a2432c94_LandingPointIcon",
            "sinet.startup.inDriver:id/map_screen_departure_marker_PointAPinPointRedesign",
            "sinet.startup.inDriver:id/map_screen_destination_marker_PassengerPointBRedesign",
            "sinet.startup.inDriver:id/map_screen_pin_marker_PointAPinHeadRedesign",
        )

    @Test
    fun `textos reales de a11y de oferta InDrive extraen monto pero distancia queda null`() {
        val parsed = parseWithDetection(realDiscoveryA11yTexts(), packageName = "sinet.startup.inDriver")

        assertNotNull(parsed.offer)
        assertEquals(3.7, parsed.offer!!.estimatedTotal ?: 0.0, 0.001)
        // Defecto FASE 10 reproducible: la fuente (a11y) no entrega distancia.
        assertNull("la oferta real no expone distancia en a11y", parsed.offer!!.distanceKm)
    }

    @Test
    fun `la duracion con NBSP se normaliza y se extrae en vez de tomar el 2 min de espera`() {
        // FASE 10-D: InDrive serializa "~18\u00A0min." con NBSP (U+00A0), que
        // `\s` no matchea. Sin normalización quedaría el "2 min" de espera
        // estimada de las tarjetas de categoría (NO la duración del viaje).
        val parser = OfferTextParser()
        assertEquals(18.0, parser.parse(listOf("~18\u00A0min.")).durationsMin.first(), 0.001)
        assertEquals(
            18.0,
            parser.parse(listOf("Av Diego V\u00E1squez de Cepeda 170120 ~18\u00A0min.")).durationsMin.first(),
            0.001,
        )
        // El "2 min" de las tarjetas de categoría sigue siendo un candidato,
        // pero la duración real (máxima) gana en el extractor.
        assertEquals(
            18.0,
            parser.parse(listOf("2 min", "~18\u00A0min.")).durationsMin.max(),
            0.001,
        )
    }

    @Test
    fun `si la misma fuente entrega una linea de distancia el parser la extrae`() {
        val texts = realDiscoveryA11yTexts() + "16,4 km"

        val parsed = parseWithDetection(texts, packageName = "sinet.startup.inDriver")

        assertNotNull(parsed.offer)
        assertEquals(16.4, parsed.offer!!.distanceKm ?: 0.0, 0.001)
    }
}
