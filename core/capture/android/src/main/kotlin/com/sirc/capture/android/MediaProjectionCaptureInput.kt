package com.sirc.capture.android

import android.graphics.Bitmap
import com.sirc.capture.android.provider.ScreenCaptureProvider
import com.sirc.capture.di.AccessibilityRequests
import com.sirc.capture.input.CaptureInput
import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.CaptureRequest
import com.sirc.core.platform.CaptureInputType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entrada de captura basada en MediaProjection (WP-E3-03).
 *
 * No captura por su cuenta: observa la corriente base (accesibilidad, ya
 * debounced) y la enriquece con la imagen real de pantalla cuando la
 * proyección está activa.
 *
 * - Si [ScreenCaptureProvider.isProjecting]: captura el frame, lo codifica a PNG
 *   y emite `request.copy(imageData = png, origin = MEDIA_PROJECTION)`.
 * - Si no proyecta, o el frame no está disponible (degrade): emite el request
 *   sin cambios (origin queda `ACCESSIBILITY`, se usan los textos).
 */
@Singleton
class MediaProjectionCaptureInput @Inject constructor(
    @AccessibilityRequests private val baseRequests: Flow<CaptureRequest>,
    private val provider: ScreenCaptureProvider,
    private val logger: SircLogger,
) : CaptureInput {
    override val origin: CaptureInputType = CaptureInputType.MEDIA_PROJECTION

    override fun requests(): Flow<CaptureRequest> = baseRequests.map { request -> enrichWithFrame(request) }

    private suspend fun enrichWithFrame(request: CaptureRequest): CaptureRequest {
        if (!provider.isProjecting.value) return request
        val png = runCatching { provider.captureFrame()?.toPngBytes() }.getOrNull()
        if (png == null) {
            logger.warn(TAG, "sin frame disponible, se usan los textos de accesibilidad")
            return request
        }
        return enrichWithImage(request, png)
    }

    companion object {
        private const val TAG = "MediaProjectionCaptureInput"
    }
}

/** Aplica la imagen capturada (PNG) y el origen [CaptureInputType.MEDIA_PROJECTION]. */
internal fun enrichWithImage(
    request: CaptureRequest,
    png: ByteArray,
): CaptureRequest = request.copy(imageData = png, origin = CaptureInputType.MEDIA_PROJECTION)

private fun Bitmap.toPngBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, output)
    return output.toByteArray()
}
