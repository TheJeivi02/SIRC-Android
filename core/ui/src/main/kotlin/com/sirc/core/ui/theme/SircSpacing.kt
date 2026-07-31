package com.sirc.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado del Design System SIRC.
 *
 * Usar siempre estos tokens en lugar de valores sueltos para mantener una
 * cuadrícula coherente en todas las pantallas y componentes.
 */
object SircSpacing {
    /** 4dp — espaciado mínimo (gaps internos, separadores). */
    val XS = 4.dp

    /** 8dp — compacto (modo compacto del overlay, filas). */
    val SM = 8.dp

    /** 12dp — ritmo estándar entre bloques relacionados. */
    val MD = 12.dp

    /** 16dp — márgenes de pantalla y tarjetas. */
    val LG = 16.dp

    /** 24dp — agrupación de secciones. */
    val XL = 24.dp

    /** 32dp — separaciones amplias. */
    val XXL = 32.dp
}
