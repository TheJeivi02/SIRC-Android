package com.sirc.capture.ocr

/**
 * Motor de reconocimiento óptico de caracteres (OCR).
 *
 * Abstracción sobre ML Kit para permitir sustituciones (por ejemplo falsos en
 * pruebas o un motor distinto) sin tocar el pipeline.
 */
interface OcrEngine {
    /** Devuelve las líneas de texto reconocidas en [imageData], vacías si no hay texto. */
    suspend fun recognize(imageData: ByteArray): List<String>
}
