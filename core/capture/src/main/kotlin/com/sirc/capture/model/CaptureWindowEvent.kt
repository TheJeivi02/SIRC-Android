package com.sirc.capture.model

/**
 * Evento inmutable de cambio de ventana observado por el servicio de
 * accesibilidad.
 *
 * La captura SOLO observa: el evento transporta metadatos del cambio y el
 * texto visible (acotado) que usará el parser en un futuro; no interpreta nada.
 */
data class CaptureWindowEvent(
    val eventId: Long,
    val packageName: String,
    val eventType: WindowEventType,
    val timestampMillis: Long,
    val textCount: Int,
    val fingerprint: String,
    val texts: List<String> = emptyList(),
)
