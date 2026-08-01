package com.sirc.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.sirc.domain.model.Decision
import com.sirc.domain.model.Recommendation

/**
 * Estados de rentabilidad del Design System.
 *
 * Traduce una [Decision] del motor a un estado visual con etiqueta y color de
 * semáforo. Es la única fuente de la semántica de color/etiqueta; los
 * componentes (p. ej. [com.sirc.core.ui.components.ProfitIndicator]) la
 * consumen.
 */
enum class ProfitState(
    val label: String,
    val color: Color,
) {
    PROFITABLE(label = "CONVIENE", color = SircColors.Profit),
    MARGINAL(label = "DUDOSO", color = SircColors.Marginal),
    NOT_PROFITABLE(label = "NO CONVIENE", color = SircColors.NotProfit),
    ;

    companion object {
        fun fromDecision(decision: Decision): ProfitState =
            when (decision) {
                Decision.PROFITABLE -> PROFITABLE
                Decision.MARGINAL -> MARGINAL
                Decision.NOT_PROFITABLE -> NOT_PROFITABLE
            }

        fun fromRecommendation(recommendation: Recommendation): ProfitState =
            when (recommendation) {
                Recommendation.ACCEPT -> PROFITABLE
                Recommendation.WARNING -> MARGINAL
                Recommendation.REJECT -> NOT_PROFITABLE
            }
    }
}

/** Etiqueta accionable de una [Recommendation] para el overlay. */
fun recommendationLabel(recommendation: Recommendation): String =
    when (recommendation) {
        Recommendation.ACCEPT -> "ACEPTAR"
        Recommendation.REJECT -> "RECHAZAR"
        Recommendation.WARNING -> "REVISAR"
    }
