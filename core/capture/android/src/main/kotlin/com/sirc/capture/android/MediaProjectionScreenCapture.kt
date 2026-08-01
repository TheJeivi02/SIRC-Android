package com.sirc.capture.android

import android.graphics.Bitmap
import com.sirc.capture.android.provider.ScreenCaptureProvider
import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.ScreenFrame
import com.sirc.capture.screen.ScreenCapture
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [ScreenCapture] basada en MediaProjection.
 *
 * Cuando la proyección está activa captura el frame real de pantalla como PNG
 * ([ScreenFrame.imageData]) para alimentar el OCR. Sin proyección (o si no hay
 * frame disponible) degrada al texto ya observado por accesibilidad.
 */
@Singleton
class MediaProjectionScreenCapture @Inject constructor(
    private val provider: ScreenCaptureProvider,
    private val logger: SircLogger,
) : ScreenCapture {
    override suspend fun capture(request: CaptureRequest): ScreenFrame? {
        val imageData =
            if (provider.isProjecting.value) {
                val bitmap = provider.captureFrame()
                val png = bitmap?.let { it.toPngBytes() }
                bitmap?.recycle()
                if (png == null) {
                    logger.warn(TAG, "sin frame disponible, se usa texto de accesibilidad")
                }
                png
            } else {
                null
            }
        return ScreenFrame(
            requestId = request.id,
            packageName = request.packageName,
            timestampMillis = request.timestampMillis,
            texts = request.texts,
            imageData = imageData,
        )
    }

    private fun Bitmap.toPngBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }

    companion object {
        private const val TAG = "MediaProjectionScreenCapture"
    }
}
