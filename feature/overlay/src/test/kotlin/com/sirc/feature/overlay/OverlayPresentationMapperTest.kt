package com.sirc.feature.overlay

import com.sirc.capture.model.OverlayState
import com.sirc.core.ui.theme.ProfitState
import com.sirc.domain.engine.ConfidenceLevel
import com.sirc.domain.engine.ConfidenceResult
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.Decision
import com.sirc.domain.model.OfferRecommendation
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayPresentationMapperTest {
    private val engine = ProfitEngine()
    private val mapper = ::mapToOverlayPresentation

    @Test
    fun `ACCEPT se muestra como ACEPTAR y estado PROFITABLE`() {
        val presentation = mapper(state(recommendation = Recommendation.ACCEPT), engine)

        assertNotNull(presentation)
        assertEquals("ACEPTAR", presentation?.decision?.label)
        assertEquals(ProfitState.PROFITABLE, presentation?.decision?.state)
    }

    @Test
    fun `REJECT se muestra como RECHAZAR y estado NOT_PROFITABLE`() {
        val presentation = mapper(state(recommendation = Recommendation.REJECT), engine)

        assertEquals("RECHAZAR", presentation?.decision?.label)
        assertEquals(ProfitState.NOT_PROFITABLE, presentation?.decision?.state)
    }

    @Test
    fun `WARNING se muestra como REVISAR y estado MARGINAL`() {
        val presentation = mapper(state(recommendation = Recommendation.WARNING), engine)

        assertEquals("REVISAR", presentation?.decision?.label)
        assertEquals(ProfitState.MARGINAL, presentation?.decision?.state)
    }

    @Test
    fun `sin recomendacion la decision cae al motor`() {
        val presentation = mapper(state(recommendation = null, decision = Decision.NOT_PROFITABLE), engine)

        assertEquals("NO CONVIENE", presentation?.decision?.label)
        assertEquals(ProfitState.NOT_PROFITABLE, presentation?.decision?.state)
    }

    @Test
    fun `la oferta expone plataforma y monto formateado`() {
        val presentation = mapper(state(), engine)

        assertEquals("InDrive", presentation?.offer?.platform)
        assertEquals("\$125", presentation?.offer?.amount)
    }

    @Test
    fun `el resumen combina distancia y duracion`() {
        val presentation = mapper(state(), engine)

        assertEquals("12.4 km · 18 min", presentation?.offer?.summary)
    }

    @Test
    fun `el resumen omite la distancia cuando es cero`() {
        val evaluation = evaluation(distanceKm = 0.0)
        val presentation = mapper(state(evaluation = evaluation), engine)

        assertEquals("18 min", presentation?.offer?.summary)
    }

    @Test
    fun `el resumen omite la duracion cuando es cero`() {
        val evaluation = evaluation(durationMin = 0.0)
        val presentation = mapper(state(evaluation = evaluation), engine)

        assertEquals("12.4 km", presentation?.offer?.summary)
    }

    @Test
    fun `el resumen es null sin distancia ni duracion`() {
        val evaluation = evaluation(distanceKm = 0.0, durationMin = 0.0)
        val presentation = mapper(state(evaluation = evaluation), engine)

        assertNull(presentation?.offer?.summary)
    }

    @Test
    fun `las metricas se agrupan en pares`() {
        val presentation = mapper(state(), engine)

        val rows = presentation?.metricRows.orEmpty()
        assertEquals(2, rows.size)
        assertEquals("GANANCIA", rows[0].left.label)
        assertEquals("\$65", rows[0].left.value)
        assertEquals("POR HORA", rows[0].right?.label)
        assertEquals("\$216/h", rows[0].right?.value)
        assertEquals("COSTO EST.", rows[1].left.label)
        assertEquals("\$60", rows[1].left.value)
        assertNull(rows[1].right)
    }

    @Test
    fun `las metricas respetan los flags desactivados`() {
        val config =
            OverlayConfig(
                showProfit = false,
                showProfitPerHour = true,
                showProfitPerKm = false,
                showTripSummary = false,
            )
        val presentation = mapper(state(config = config), engine)

        val rows = presentation?.metricRows.orEmpty()
        assertEquals(1, rows.size)
        assertEquals("POR HORA", rows[0].left.label)
        assertNull(rows[0].right)
    }

    @Test
    fun `tono positivo cuando la ganancia es alta`() {
        val presentation = mapper(state(), engine)

        assertEquals(MetricTone.POSITIVE, presentation?.metricRows?.first()?.left?.tone)
    }

    @Test
    fun `tono negativo cuando la ganancia no es positiva`() {
        val evaluation = evaluation(estimatedProfit = -5.0, totalCost = 60.0)
        val presentation = mapper(state(evaluation = evaluation), engine)

        assertEquals(MetricTone.NEGATIVE, presentation?.metricRows?.first()?.left?.tone)
    }

    @Test
    fun `tono neutro en el resto`() {
        val evaluation = evaluation(estimatedProfit = 20.0, totalCost = 60.0)
        val presentation = mapper(state(evaluation = evaluation), engine)

        assertEquals(MetricTone.NEUTRAL, presentation?.metricRows?.first()?.left?.tone)
    }

    @Test
    fun `por hora y por km heredan el tono de la ganancia`() {
        val config = OverlayConfig(showProfitPerKm = true)
        val presentation = mapper(state(config = config), engine)

        val gain = presentation?.metricRows?.first()?.left?.tone
        val perHour = presentation?.metricRows?.first()?.right?.tone
        val perKm = presentation?.metricRows?.get(1)?.left?.tone
        assertEquals(gain, perHour)
        assertEquals(gain, perKm)
    }

    @Test
    fun `costo estimado usa tono apagado`() {
        val presentation = mapper(state(), engine)

        assertEquals(MetricTone.MUTED, presentation?.metricRows?.get(1)?.left?.tone)
    }

    @Test
    fun `la linea secundaria usa motivo y confianza de la recomendacion`() {
        val presentation = mapper(state(recommendation = Recommendation.ACCEPT), engine)

        assertEquals("Supera los umbrales configurados · 85% confianza", presentation?.secondaryLine)
    }

    @Test
    fun `la linea secundaria muestra informacion insuficiente sin recomendacion`() {
        val confidence = ConfidenceResult(level = ConfidenceLevel.LOW, percent = 40, reasons = emptyList())
        val presentation = mapper(state(recommendation = null, confidence = confidence), engine)

        assertEquals("Información insuficiente · 40% confianza", presentation?.secondaryLine)
    }

    @Test
    fun `la linea secundaria muestra confianza accionable sin recomendacion`() {
        val confidence = ConfidenceResult(level = ConfidenceLevel.HIGH, percent = 90, reasons = emptyList())
        val presentation =
            mapper(state(recommendation = null, confidence = confidence, offerType = "UBER_REQUEST"), engine)

        assertEquals("UBER_REQUEST · Confianza 90% (HIGH)", presentation?.secondaryLine)
    }

    @Test
    fun `sin evaluacion el mapper devuelve null`() {
        val presentation = mapper(OverlayUiState(status = OverlayState.WAITING, visible = true), engine)

        assertNull(presentation)
    }

    @Test
    fun `showDecision desactivado oculta la decision pero conserva oferta y metricas`() {
        val config = OverlayConfig(showDecision = false)
        val presentation = mapper(state(config = config), engine)

        assertNull(presentation?.decision)
        assertNotNull(presentation?.offer)
        assertEquals(2, presentation?.metricRows?.size)
    }

    @Test
    fun `montos con decimales largos se redondean a dos digitos`() {
        val evaluation = evaluation(estimatedProfit = 4.0867, totalCost = 60.0, estimatedTotal = 125.0)
        val config = OverlayConfig(showProfitPerHour = true, showTripSummary = false)
        val presentation = mapper(state(evaluation = evaluation, config = config), engine)

        assertEquals("\$4.09", presentation?.metricRows?.first()?.left?.value)
        assertEquals("\$216/h", presentation?.metricRows?.first()?.right?.value)
    }

    private fun state(
        recommendation: Recommendation? = Recommendation.ACCEPT,
        decision: Decision = Decision.PROFITABLE,
        evaluation: ProfitEvaluation = evaluation(decision = decision),
        config: OverlayConfig = OverlayConfig(),
        confidence: ConfidenceResult? =
            ConfidenceResult(level = ConfidenceLevel.HIGH, percent = 90, reasons = emptyList()),
        offerType: String? = "INDRIVE_REQUEST",
    ): OverlayUiState =
        OverlayUiState(
            evaluation = evaluation,
            recommendation =
                recommendation?.let {
                    OfferRecommendation(
                        it,
                        "Supera los umbrales configurados",
                        emptyList(),
                        85,
                    )
                },
            config = config,
            status = OverlayState.WAITING,
            visible = true,
            offerType = offerType,
            confidence = confidence,
        )

    private fun evaluation(
        estimatedTotal: Double = 125.0,
        distanceKm: Double = 12.4,
        durationMin: Double = 18.0,
        totalCost: Double = 60.0,
        estimatedProfit: Double = 65.0,
        profitPerHour: Double = 216.0,
        decision: Decision = Decision.PROFITABLE,
    ): ProfitEvaluation =
        ProfitEvaluation(
            offer =
                TripOffer(
                    platform = RidePlatform.INDRIVE,
                    timestampMillis = 0,
                    estimatedTotal = estimatedTotal,
                    distanceKm = distanceKm,
                    durationMin = durationMin,
                    currency = "USD",
                ),
            metrics =
                ProfitMetrics(
                    estimatedTotal = estimatedTotal,
                    distanceKm = distanceKm,
                    durationMin = durationMin,
                    totalCost = totalCost,
                    estimatedProfit = estimatedProfit,
                    profitPerKm = if (distanceKm > 0) estimatedProfit / distanceKm else 0.0,
                    profitPerHour = profitPerHour,
                    marginPercent = 0.0,
                ),
            decision = decision,
            reasons = listOf("Supera los umbrales configurados"),
        )
}
