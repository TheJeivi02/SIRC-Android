package com.sirc.capture.screen

import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.ScreenFrame

/**
 * Captura el contenido visual de una solicitud.
 *
 * Hoy la fuente real es la accesibilidad (texto ya observado); una futura
 * implementación con MediaProjection añadirá también la imagen para el OCR.
 */
interface ScreenCapture {
    suspend fun capture(request: CaptureRequest): ScreenFrame?
}
