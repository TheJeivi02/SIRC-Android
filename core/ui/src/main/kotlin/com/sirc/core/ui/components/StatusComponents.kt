package com.sirc.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirc.core.ui.theme.SircColors
import com.sirc.domain.model.Decision

/**
 * Insignia de decisión. Colores semáforo para reconocimiento instantáneo.
 */
@Composable
fun DecisionBadge(
    decision: Decision,
    compact: Boolean = false,
) {
    val color =
        when (decision) {
            Decision.PROFITABLE -> SircColors.Profit
            Decision.MARGINAL -> SircColors.Marginal
            Decision.NOT_PROFITABLE -> SircColors.NotProfit
        }
    val label =
        when (decision) {
            Decision.PROFITABLE -> "CONVIENE"
            Decision.MARGINAL -> "DUDOSO"
            Decision.NOT_PROFITABLE -> "NO CONVIENE"
        }

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 2.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
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
        androidx.compose.foundation.layout.Box(
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
