package com.sirc.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirc.core.ui.theme.ProfitState
import com.sirc.core.ui.theme.SircColors
import com.sirc.core.ui.theme.SircTheme
import com.sirc.core.ui.theme.recommendationLabel
import com.sirc.domain.model.Decision
import com.sirc.domain.model.Recommendation

/**
 * Insignia de decisión del motor. Delga en [ProfitIndicator] usando el estado
 * derivado de la [Decision].
 */
@Composable
fun DecisionBadge(
    decision: Decision,
    compact: Boolean = false,
) {
    ProfitIndicator(
        state = ProfitState.fromDecision(decision),
        compact = compact,
    )
}

/**
 * Insignia de recomendación accionable. Usa el color del estado semáforo
 * derivado de la [Recommendation] y la etiqueta ACEPTAR/RECHAZAR/REVISAR.
 */
@Composable
fun RecommendationBadge(
    recommendation: Recommendation,
    compact: Boolean = false,
) {
    ProfitIndicator(
        state = ProfitState.fromRecommendation(recommendation),
        compact = compact,
        label = recommendationLabel(recommendation),
    )
}

/** Indica el estado de un servicio (Overlay, Accesibilidad). */
@Composable
fun StatusDot(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) SircColors.Profit else SircColors.OnDarkMuted)
                    .padding(5.dp),
        )
        Text(
            text = if (active) "Activo" else "Inactivo",
            color = if (active) SircColors.Profit else SircColors.OnDarkMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DecisionBadgePreview() {
    SircTheme {
        Column {
            DecisionBadge(decision = Decision.PROFITABLE)
            DecisionBadge(decision = Decision.MARGINAL)
            DecisionBadge(decision = Decision.NOT_PROFITABLE)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusDotPreview() {
    SircTheme {
        Column {
            StatusDot(active = true)
            StatusDot(active = false)
        }
    }
}
