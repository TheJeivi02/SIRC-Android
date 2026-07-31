package com.sirc.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Elevaciones del Design System SIRC.
 *
 * El overlay y las tarjetas priorizan contraste con borde/fondo sobre sombras;
 * estos tokens existen para mantener elevaciones coherentes donde sí apliquen.
 */
object SircElevations {
    /** Tarjetas de sección (Home, Ajustes, Historial). */
    val Card = 2.dp

    /** Tarjetas destacadas sobre el fondo de pantalla. */
    val CardProminent = 4.dp

    /** Overlay flotante sobre apps de terceros. */
    val Overlay = 8.dp
}
