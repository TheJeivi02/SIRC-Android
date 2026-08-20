package com.sirc.feature.overlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sirc.capture.model.OverlayState
import com.sirc.core.ui.theme.SircTheme
import com.sirc.domain.engine.ConfidenceLevel
import com.sirc.domain.engine.ConfidenceResult
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.model.Decision
import com.sirc.domain.model.GoalStatus
import com.sirc.domain.model.OfferRecommendation
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.ProfitEvaluation
import com.sirc.domain.model.ProfitMetrics
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.model.TripOffer
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented test (device) del render real de [OverlayContent].
 *
 * Demuestra que el Composable renderiza el estado esperado en pantalla
 * (observable), no los detalles internos del mapper (que ya cubren los unit
 * tests). No se añaden test tags a producción: se seleccionan los nodos por su
 * texto real.
 */
class OverlayContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = ProfitEngine()

    @Test
    fun sinEvaluacion_muestraElEstadoDelPipeline() {
        compose.setContent {
            SircTheme {
                OverlayContent(
                    state =
                        OverlayUiState(
                            evaluation = null,
                            config = OverlayConfig(),
                            status = OverlayState.WAITING,
                            visible = true,
                        ),
                    engine = engine,
                    onDismiss = {},
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("Esperando oferta…").assertIsDisplayed()
        compose.onNodeWithText("ACEPTAR").assertDoesNotExist()
    }

    @Test
    fun ofertaValida_muestraDecisionOfertaYMetricas() {
        compose.setContent {
            SircTheme {
                OverlayContent(state = state(), engine = engine, onDismiss = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("ACEPTAR").assertIsDisplayed()
        compose.onNodeWithText("InDrive").assertIsDisplayed()
        compose.onNodeWithText("$125").assertIsDisplayed()
        compose.onNodeWithText("12.4 km · 18 min").assertIsDisplayed()
        compose.onNodeWithText("GANANCIA").assertIsDisplayed()
        compose.onNodeWithText("POR HORA").assertIsDisplayed()
        compose.onNodeWithText("COSTO EST.").assertIsDisplayed()
    }

    @Test
    fun showDecisionDesactivado_ocultaLaDecisionSinAlterarLaEvaluacion() {
        compose.setContent {
            SircTheme {
                OverlayContent(
                    state = state(config = OverlayConfig(showDecision = false)),
                    engine = engine,
                    onDismiss = {},
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("ACEPTAR").assertDoesNotExist()
        compose.onNodeWithText("$125").assertIsDisplayed()
        compose.onNodeWithText("GANANCIA").assertIsDisplayed()
    }

    @Test
    fun showDecisionActivado_muestraLaDecisionCuandoExiste() {
        compose.setContent {
            SircTheme {
                OverlayContent(
                    state = state(config = OverlayConfig(showDecision = true)),
                    engine = engine,
                    onDismiss = {},
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("ACEPTAR").assertIsDisplayed()
    }

    @Test
    fun compactMode_respetaLosMismosIndicadores() {
        compose.setContent {
            SircTheme {
                OverlayContent(
                    state = state(config = OverlayConfig(compactMode = true)),
                    engine = engine,
                    onDismiss = {},
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("ACEPTAR").assertIsDisplayed()
        compose.onNodeWithText("$125").assertIsDisplayed()
        compose.onNodeWithText("GANANCIA").assertIsDisplayed()
        compose.onNodeWithText("POR HORA").assertIsDisplayed()
        compose.onNodeWithText("COSTO EST.").assertIsDisplayed()
    }

    @Test
    fun datosFaltantes_noMuestraMetricasInventadas() {
        compose.setContent {
            SircTheme {
                OverlayContent(
                    state =
                        state(
                            evaluation = evaluation(distanceKm = 0.0, durationMin = 0.0),
                        ),
                    engine = engine,
                    onDismiss = {
                    },
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("$125").assertIsDisplayed()
        compose.onNodeWithText("ACEPTAR").assertIsDisplayed()
        compose.onNodeWithText("GANANCIA").assertDoesNotExist()
        compose.onNodeWithText("POR HORA").assertDoesNotExist()
        compose.onNodeWithText("POR KM").assertDoesNotExist()
        compose.onNodeWithText("COSTO EST.").assertDoesNotExist()
        compose.onNodeWithText("12.4 km · 18 min").assertDoesNotExist()
    }

    private fun state(
        evaluation: ProfitEvaluation = evaluation(),
        config: OverlayConfig = OverlayConfig(),
    ): OverlayUiState =
        OverlayUiState(
            evaluation = evaluation,
            recommendation =
                OfferRecommendation(
                    Recommendation.ACCEPT,
                    "Supera los umbrales configurados",
                    emptyList(),
                    85,
                ),
            config = config,
            status = OverlayState.WAITING,
            visible = true,
            offerType = "INDRIVE_REQUEST",
            confidence = ConfidenceResult(level = ConfidenceLevel.HIGH, percent = 90, reasons = emptyList()),
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
            } else {
                GoalStatus.FAILED
            },
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
            decision = Decision.PROFITABLE,
            reasons = listOf("Supera los umbrales configurados"),
        )
    }
}
