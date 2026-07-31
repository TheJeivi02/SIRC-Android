package com.sirc.capture.model

/**
 * Contenido capturado de la pantalla, listo para el siguiente paso del
 * pipeline (OCR sobre [imageData] o parseo directo de [texts]).
 */
data class ScreenFrame(
    val requestId: Long,
    val packageName: String,
    val timestampMillis: Long,
    val texts: List<String> = emptyList(),
    val imageData: ByteArray? = null,
)
