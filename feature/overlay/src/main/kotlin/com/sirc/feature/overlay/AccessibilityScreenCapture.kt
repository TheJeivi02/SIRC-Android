package com.sirc.feature.overlay

import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.ScreenFrame
import com.sirc.capture.screen.ScreenCapture
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [ScreenCapture] que toma el contenido ya observado por
 * accesibilidad (texto). Preparada para, en un futuro sprint, añadir captura
 * de imagen real (MediaProjection) que alimente el OCR.
 */
@Singleton
class AccessibilityScreenCapture @Inject constructor() : ScreenCapture {
    override suspend fun capture(request: CaptureRequest): ScreenFrame =
        ScreenFrame(
            requestId = request.id,
            packageName = request.packageName,
            timestampMillis = request.timestampMillis,
            texts = request.texts,
            imageData = request.imageData,
        )
}
