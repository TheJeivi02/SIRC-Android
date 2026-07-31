package com.sirc.capture.model

/**
 * Solicitud de captura generada a partir de un evento observado.
 *
 * Transporta el contenido disponible de la pantalla: el texto visible
 * (accesibilidad) y, en el futuro, la imagen capturada ([imageData]) que
 * alimentará al OCR. No interpreta nada: solo transporta datos.
 */
data class CaptureRequest(
    val id: Long,
    val packageName: String,
    val timestampMillis: Long,
    val texts: List<String> = emptyList(),
    val imageData: ByteArray? = null,
)
