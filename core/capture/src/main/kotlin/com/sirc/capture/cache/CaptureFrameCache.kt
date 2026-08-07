package com.sirc.capture.cache

import com.sirc.capture.model.CaptureRequest

/**
 * Caché de frames ya procesados basada en el hash del contenido capturado.
 *
 * Evita volver a ejecutar OCR/parseo sobre capturas idénticas (el mismo
 * contenido de pantalla). Solo deduplica cuando el request lleva imagen; un
 * request sin imagen (texto de accesibilidad) siempre se considera nuevo.
 */
interface CaptureFrameCache {
    /** `true` si el request nunca se procesó. */
    fun isNew(request: CaptureRequest): Boolean

    /** Marca el request como procesado. */
    fun markProcessed(request: CaptureRequest)

    fun clear()
}
