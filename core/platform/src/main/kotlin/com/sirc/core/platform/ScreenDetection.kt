package com.sirc.core.platform

/**
 * Resultado de clasificar el texto visible en una pantalla del conductor.
 *
 * [type] es el estado detectado; [matchedKeywords] y [confidence] (0.0-1.0)
 * permiten inspeccionar por qué se llegó a esa clasificación (panel de
 * depuración y tests).
 */
data class ScreenDetection(
    val type: ScreenType,
    val matchedKeywords: List<String> = emptyList(),
    val confidence: Float = 0f,
) {
    val isRequest: Boolean
        get() = type == ScreenType.REQUEST
}
