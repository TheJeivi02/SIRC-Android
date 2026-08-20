package com.sirc.feature.overlay

import com.sirc.capture.model.OverlayState
import com.sirc.core.ui.theme.ProfitState
import com.sirc.domain.engine.ConfidenceLevel
import com.sirc.domain.engine.ConfidenceResult
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.engine.RecommendationEngine
import com.sirc.domain.model.Decision
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.GoalStatus
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
import org.junit.Assert.assertTrue
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
    fun `ganancia positiva se pinta verde`() {
        val presentation = mapper(state(), engine)

        assertEquals(MetricTone.POSITIVE, presentation?.metricRows?.first()?.left?.tone)
    }

    @Test
    fun `perdida real pinta la ganancia en rojo`() {
        val evaluation = evaluation(estimatedProfit = -5.0, totalCost = 60.0, decision = Decision.NOT_PROFITABLE)
        val presentation = mapper(state(evaluation = evaluation, recommendation = Recommendation.REJECT), engine)

        assertEquals(MetricTone.NEGATIVE, presentation?.metricRows?.first()?.left?.tone)
    }

    @Test
    fun `ganancia al limite se pinta naranja`() {
        val evaluation = evaluation(estimatedProfit = 0.0, totalCost = 60.0, decision = Decision.MARGINAL)
        val presentation = mapper(state(evaluation = evaluation, recommendation = Recommendation.WARNING), engine)

        assertEquals(MetricTone.WARNING, presentation?.metricRows?.first()?.left?.tone)
    }

    @Test
    fun `cada metrica usa el tono de su propio objetivo`() {
        val config = OverlayConfig(showProfitPerKm = true)
        val evaluation =
            evaluation(
                hourlyGoal = GoalStatus.NEAR,
                kmGoal = GoalStatus.FAILED,
                decision = Decision.MARGINAL,
            )
        val presentation = mapper(state(evaluation = evaluation, config = config), engine)

        val gain = presentation?.metricRows?.first()?.left?.tone
        val perHour = presentation?.metricRows?.first()?.right?.tone
        val perKm = presentation?.metricRows?.get(1)?.left?.tone
        assertEquals(MetricTone.WARNING, perHour)
        assertEquals(MetricTone.NEGATIVE, perKm)
        assertEquals(MetricTone.POSITIVE, gain)
    }

    @Test
    fun `costo estimado usa tono apagado`() {
        val presentation = mapper(state(), engine)

        assertEquals(MetricTone.MUTED, presentation?.metricRows?.get(1)?.left?.tone)
    }

    @Test
    fun `por hora se pinta verde cuando cumple aunque la decision sea DUDOSO`() {
        // Oferta real InDrive: monto + duración sin distancia. La decisión es
        // REVISAR (falta distancia) pero $/hora cumple el objetivo => verde.
        val evaluation =
            evaluation(
                distanceKm = 0.0,
                durationMin = 27.0,
                estimatedProfit = 5.9,
                profitPerHour = 13.1,
                hourlyGoal = GoalStatus.MET,
                kmGoal = null,
                decision = Decision.MARGINAL,
            )
        val presentation =
            mapper(state(evaluation = evaluation, recommendation = Recommendation.WARNING), engine)

        assertEquals(ProfitState.MARGINAL, presentation?.decision?.state)
        val rows = presentation?.metricRows.orEmpty()
        assertEquals(1, rows.size)
        assertEquals("POR HORA", rows[0].left.label)
        assertEquals(MetricTone.POSITIVE, rows[0].left.tone)
    }

    @Test
    fun `sin distancia se ocultan las metricas que dependen de ella pero no por hora`() {
        val config =
            OverlayConfig(
                showProfit = true,
                showProfitPerHour = true,
                showProfitPerKm = true,
                showTripSummary = true,
            )
        val evaluation = evaluation(distanceKm = 0.0, durationMin = 27.0, decision = Decision.MARGINAL)
        val presentation =
            mapper(state(evaluation = evaluation, recommendation = Recommendation.WARNING, config = config), engine)

        val labels =
            presentation?.metricRows.orEmpty().flatMap { listOfNotNull(it.left.label, it.right?.label) }
        assertEquals(listOf("POR HORA"), labels)
    }

    @Test
    fun `sin duracion se oculta por hora pero se conserva por km`() {
        val config =
            OverlayConfig(
                showProfit = true,
                showProfitPerHour = true,
                showProfitPerKm = true,
                showTripSummary = true,
            )
        val evaluation = evaluation(distanceKm = 12.4, durationMin = 0.0, decision = Decision.MARGINAL)
        val presentation =
            mapper(state(evaluation = evaluation, recommendation = Recommendation.WARNING, config = config), engine)

        val labels =
            presentation?.metricRows.orEmpty().flatMap { listOfNotNull(it.left.label, it.right?.label) }
        assertEquals(listOf("GANANCIA", "POR KM", "COSTO EST."), labels)
    }

    @Test
    fun `oferta solo precio no muestra metricas inventadas`() {
        val evaluation = evaluation(distanceKm = 0.0, durationMin = 0.0)
        val presentation = mapper(state(evaluation = evaluation), engine)

        assertTrue(presentation?.metricRows.orEmpty().isEmpty())
        assertEquals("\$125", presentation?.offer?.amount)
        assertNull(presentation?.offer?.summary)
    }

    @Test
    fun `caso 590 en 27 minutos sin distancia se muestra como REVISAR con por hora en verde`() {
        val offer =
            TripOffer(
                platform = RidePlatform.INDRIVE,
                timestampMillis = 0,
                estimatedTotal = 5.90,
                distanceKm = 0.0,
                durationMin = 27.0,
                currency = "USD",
            )
        val evaluation =
            engine.evaluate(
                offer,
                DriverCosts(costPerKm = 0.5, costPerTrip = 0.0),
                DecisionThresholds(minProfitPerKm = 0.5, minProfitPerHour = 11.0),
            )
        val recommendation = RecommendationEngine().recommend(evaluation)
        val presentation =
            mapper(
                OverlayUiState(
                    evaluation = evaluation,
                    recommendation = recommendation,
                    config = OverlayConfig(),
                    status = OverlayState.WAITING,
                    visible = true,
                    offerType = "INDRIVE_REQUEST",
                    confidence =
                        ConfidenceResult(level = ConfidenceLevel.HIGH, percent = 90, reasons = emptyList()),
                ),
                engine,
            )

        assertEquals("REVISAR", presentation?.decision?.label)
        assertEquals(ProfitState.MARGINAL, presentation?.decision?.state)
        assertEquals("\$5.9", presentation?.offer?.amount)
        assertEquals("27 min", presentation?.offer?.summary)
        val rows = presentation?.metricRows.orEmpty()
        assertEquals(1, rows.size)
        assertEquals("POR HORA", rows[0].left.label)
        assertEquals("\$13.11/h", rows[0].left.value)
        assertEquals(MetricTone.POSITIVE, rows[0].left.tone)
    }

    @Test
    fun `compactMode no altera los indicadores activos`() {
        val base = mapper(state(), engine)
        val compact = mapper(state(config = OverlayConfig(compactMode = true)), engine)

        assertEquals(base?.decision?.label, compact?.decision?.label)
        assertEquals(base?.decision?.state, compact?.decision?.state)
        assertEquals(base?.offer?.platform, compact?.offer?.platform)
        assertEquals(base?.offer?.amount, compact?.offer?.amount)
        assertEquals(base?.offer?.summary, compact?.offer?.summary)
        assertEquals(
            base?.metricRows.orEmpty().map { it.left.label to it.right?.label },
            compact?.metricRows.orEmpty().map { it.left.label to it.right?.label },
        )
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
        hourlyGoal: GoalStatus? = if (durationMin > 0.0) GoalStatus.MET else null,
        kmGoal: GoalStatus? = if (distanceKm > 0.0) GoalStatus.MET else null,
        netGoal: GoalStatus =
            if (estimatedProfit > 0.0) {
                GoalStatus.MET
            } else if (estimatedProfit == 0.0) {
                GoalStatus.NEAR
            } else {
                GoalStatus.FAILED
            },
        decision: Decision = Decision.PROFITABLE,
    ): ProfitEvaluation {
        val hasDistance = distanceKm > 0.0
        val hasDuration = durationMin > 0.0
        return ProfitEvaluation(
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
                    profitPerKm = if (hasDistance) estimatedProfit / distanceKm else null,
                    profitPerHour = if (hasDuration) profitPerHour else null,
                    profitPerHourGoal = hourlyGoal,
                    profitPerKmGoal = kmGoal,
                    netGoal = netGoal,
                    marginPercent = 0.0,
                ),
            decision = decision,
            reasons = listOf("Supera los umbrales configurados"),
        )
    }
}
