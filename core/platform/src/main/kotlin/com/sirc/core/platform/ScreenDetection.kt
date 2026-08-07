package com.sirc.core.platform

/**
 * Resultado de clasificar el texto visible en una pantalla del conductor.
 *
 * [type] es el estado detectado; [confidence] (0.0-1.0) normaliza la suma de
 * pesos de las palabras detectadas y se usa para desempatar pantallas.
 */
data class ScreenDetection(
    val type: ScreenType,
    val confidence: Float = 0f,
) {
    val isRequest: Boolean
        get() = type == ScreenType.REQUEST
}
