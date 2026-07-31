package com.sirc.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.sirc.core.ui.theme.SircColors
import com.sirc.core.ui.theme.SircSpacing
import com.sirc.core.ui.theme.SircTheme

/** Valor de métrica ya formateado para mostrar (etiqueta + texto + color). */
data class MetricValue(
    val label: String,
    val value: String,
    val valueColor: Color,
)

/**
 * Celda de métrica: etiqueta pequeña y valor grande en color. Usada por el
 * overlay para ganancia, ganancia/hora y ganancia/km.
 */
@Composable
fun MetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier.padding(top = if (compact) SircSpacing.XS else SircSpacing.SM),
    ) {
        Text(
            text = label,
            color = SircColors.OnDarkMuted,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = if (compact) 16.sp else 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MetricCellPreview() {
    SircTheme {
        Row {
            MetricCell(label = "GANANCIA", value = "$95.5", valueColor = SircColors.Profit)
            MetricCell(label = "POR HORA", value = "$127.33/h", valueColor = SircColors.OnDark, compact = true)
        }
    }
}
