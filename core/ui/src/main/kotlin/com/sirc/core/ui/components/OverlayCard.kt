package com.sirc.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirc.core.ui.theme.SircColors
import com.sirc.core.ui.theme.SircElevations
import com.sirc.core.ui.theme.SircSpacing
import com.sirc.core.ui.theme.SircTheme

/**
 * Tarjeta base del overlay flotante. Presentacional: contenedor oscuro con
 * borde, elevación y opacidad configurable. No conoce de dominio.
 *
 * @param opacityPercent opacidad del fondo (0-100), p. ej. desde [com.sirc.domain.model.OverlayConfig.opacityPercent].
 * @param compact modo compacto (menos padding).
 */
@Composable
fun OverlayCard(
    modifier: Modifier = Modifier,
    opacityPercent: Int = 100,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val alpha = (opacityPercent / 100f).coerceIn(0.15f, 1f)

    Column(
        modifier =
            modifier
                .shadow(SircElevations.Overlay, shape)
                .clip(shape)
                .background(SircColors.OverlayBackground.copy(alpha = alpha))
                .border(1.dp, SircColors.OverlayBorder, shape)
                .padding(if (compact) SircSpacing.SM else SircSpacing.MD),
        content = content,
    )
}

/**
 * Cabecera + cuerpo del overlay. Muestra el título (plataforma) y opcionalmente
 * el botón de cierre, seguido del contenido provisto.
 */
@Composable
fun OverlayCardContent(
    title: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                color = SircColors.OnDarkMuted,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Medium,
            )
            if (onDismiss != null) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Ocultar overlay",
                    tint = SircColors.OnDarkMuted,
                    modifier =
                        Modifier
                            .clickable(onClick = onDismiss)
                            .padding(2.dp),
                )
            }
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun OverlayCardPreview() {
    SircTheme {
        OverlayCard(opacityPercent = 100) {
            OverlayCardContent(title = "Uber", onDismiss = {}) {
                Text(
                    text = "CONVIENE",
                    color = SircColors.Profit,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$95.5",
                    color = SircColors.OnDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
