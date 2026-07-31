package com.sirc.data.local.mapper

import com.sirc.domain.model.AdditionalCost
import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Test

class DriverConfigCodecTest {
    @Test
    fun `plataformas se codifican y decodifican sin perdida`() {
        val platforms = setOf(RidePlatform.UBER, RidePlatform.INDRIVE, RidePlatform.DIDI)

        val decoded = decodePlatforms(encodePlatforms(platforms))

        assertEquals(platforms, decoded)
    }

    @Test
    fun `plataformas vacias se codifican como texto vacio`() {
        assertEquals("", encodePlatforms(emptySet()))
        assertEquals(emptySet<RidePlatform>(), decodePlatforms(""))
    }

    @Test
    fun `entradas desconocidas se ignoran al decodificar`() {
        assertEquals(setOf(RidePlatform.UBER), decodePlatforms("UBER,PLATAFORMA_FANTASMA,"))
    }

    @Test
    fun `costos adicionales se codifican y decodifican sin perdida`() {
        val costs =
            listOf(
                AdditionalCost(label = "Peaje", costPerKm = 0.05),
                AdditionalCost(label = "Estacionamiento", costPerKm = 0.02),
            )

        val decoded = decodeAdditionalCosts(encodeAdditionalCosts(costs))

        assertEquals(costs, decoded)
    }

    @Test
    fun `costos con separadores en la etiqueta no rompen la codificacion`() {
        val costs = listOf(AdditionalCost(label = "Peaje: autopista", costPerKm = 1.25))

        assertEquals(costs, decodeAdditionalCosts(encodeAdditionalCosts(costs)))
    }

    @Test
    fun `costos vacios se codifican como texto vacio`() {
        assertEquals("", encodeAdditionalCosts(emptyList()))
        assertEquals(emptyList<AdditionalCost>(), decodeAdditionalCosts(""))
    }

    @Test
    fun `filas invalidas se omiten al decodificar`() {
        val decoded = decodeAdditionalCosts("label\u001Fno-numero\u001E\u001F5")

        assertEquals(listOf(AdditionalCost(label = "", costPerKm = 5.0)), decoded)
    }
}
