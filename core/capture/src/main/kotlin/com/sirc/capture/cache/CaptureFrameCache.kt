package com.sirc.capture.cache

import com.sirc.capture.model.ScreenFrame

/**
 * Caché de frames ya procesados basada en el hash del contenido capturado.
 *
 * Evita volver a ejecutar OCR/parseo sobre capturas idénticas (el mismo
 * contenido de pantalla). Solo deduplica cuando el frame lleva imagen; un
 * frame sin imagen (texto de accesibilidad) siempre se considera nuevo.
 */
interface CaptureFrameCache {
    /** `true` si el frame nunca se procesó (o no es deduplicable). */
    fun isNew(frame: ScreenFrame): Boolean

    /** Marca el frame como procesado. */
    fun markProcessed(frame: ScreenFrame)

    fun clear()
}
