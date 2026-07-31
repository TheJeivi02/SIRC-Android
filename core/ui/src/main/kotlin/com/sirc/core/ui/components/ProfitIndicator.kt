package com.sirc.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirc.core.ui.theme.ProfitState
import com.sirc.core.ui.theme.SircTheme

/**
 * Indicador de rentabilidad: píldora de color semáforo con la etiqueta del
 * estado. Es el elemento que el conductor reconoce de un vistazo (<3 s).
 */
@Composable
fun ProfitIndicator(
    state: ProfitState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(state.color)
                .padding(
                    horizontal = if (compact) 6.dp else 10.dp,
                    vertical = if (compact) 2.dp else 4.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.label,
            color = Color.White,
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfitIndicatorPreview() {
    SircTheme {
        Column {
            ProfitIndicator(state = ProfitState.PROFITABLE)
            ProfitIndicator(state = ProfitState.MARGINAL)
            ProfitIndicator(state = ProfitState.NOT_PROFITABLE, compact = true)
        }
    }
}
