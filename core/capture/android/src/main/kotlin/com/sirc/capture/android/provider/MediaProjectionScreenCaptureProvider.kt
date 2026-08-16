package com.sirc.capture.android.provider

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import com.sirc.capture.android.projection.MediaProjectionService
import com.sirc.capture.log.SircLogger
import com.sirc.capture.validation.ValidationEvent
import com.sirc.capture.validation.ValidationRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [ScreenCaptureProvider] basada en MediaProjection.
 *
 * Gestiona el token de proyección, el virtual display y el [ImageReader] que
 * entrega los frames de pantalla. En Android 14+ el token se obtiene desde el
 * [MediaProjectionService] (FGS `mediaProjection`) para cumplir el requisito
 * de que la proyección solo se active con un servicio en primer plano.
 */
@Singleton
class MediaProjectionScreenCaptureProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: SircLogger,
    private val validationRecorder: ValidationRecorder,
) : ScreenCaptureProvider {
    private val lifecycle = ProjectionLifecycle()
    private val _isProjecting = MutableStateFlow(false)
    override val isProjecting: StateFlow<Boolean> = _isProjecting.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val frames = Channel<Image>(Channel.CONFLATED)

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projectionCallback: MediaProjection.Callback? = null

    private fun syncIsProjecting() {
        _isProjecting.value = lifecycle.isActive
    }

    override fun onProjectionPermissionGranted(
        resultCode: Int,
        data: Intent?,
    ) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            logger.warn(TAG, "permiso de captura de pantalla denegado")
            return
        }
        MediaProjectionService.start(context, resultCode, data)
    }

    /**
     * Llamado por el [MediaProjectionService] una vez el FGS está activo
     * (obligatorio en Android 14+). Operación completamente atómica respaldada
     * por [ProjectionLifecycle].
     */
    fun initializeProjection(
        resultCode: Int,
        data: Intent,
    ) {
        val token = lifecycle.begin()
        syncIsProjecting()

        try {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, data)
            if (projection == null) {
                logger.error(TAG, "no se pudo obtener el token de proyección")
                validationRecorder.record(
                    ValidationEvent.CaptureError(System.currentTimeMillis(), "token de proyección no disponible"),
                )
                lifecycle.abort(token)
                syncIsProjecting()
                MediaProjectionService.stop(context)
                return
            }

            releaseResources()
            mediaProjection = projection

            val callback =
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        if (!lifecycle.isCurrent(token) || mediaProjection !== projection) return
                        logger.info(TAG, "proyección interrumpida por el sistema")
                        validationRecorder.record(
                            ValidationEvent.CaptureError(
                                System.currentTimeMillis(),
                                "proyección interrumpida por el sistema",
                            ),
                        )
                        stopProjection()
                    }
                }
            projectionCallback = callback
            projection.registerCallback(callback, mainHandler)
            startVirtualDisplay(projection)

            if (lifecycle.activate(token)) {
                syncIsProjecting()
                logger.info(TAG, "captura de pantalla activa")
            } else {
                releaseResources()
                lifecycle.abort(token)
                syncIsProjecting()
            }
        } catch (error: Throwable) {
            logger.error(TAG, "error crítico durante la inicialización de proyección: ${error.message}")
            validationRecorder.record(
                ValidationEvent.CaptureError(
                    System.currentTimeMillis(),
                    "excepción al inicializar proyección: ${error.message}",
                ),
            )
            releaseResources()
            lifecycle.abort(token)
            syncIsProjecting()
            MediaProjectionService.stop(context)
        }
    }

    override fun stopProjection() {
        lifecycle.stop()
        releaseResources()
        syncIsProjecting()
        MediaProjectionService.stop(context)
        logger.info(TAG, "captura de pantalla detenida")
    }

    /**
     * Llamado por el [MediaProjectionService] cuando el servicio se destruye
     * (fin de ciclo de vida, `stopService` externo o interrupción del sistema).
     *
     * Libera todos los recursos adquiridos durante la proyección
     * ([MediaProjection], [VirtualDisplay], [ImageReader], callbacks) para que
     * no queden fugas si el servicio finaliza sin pasar por [stopProjection].
     * Idempotente: puede invocarse varias veces sin efectos adversos.
     */
    fun onServiceDestroyed() {
        lifecycle.stop()
        releaseResources()
        syncIsProjecting()
        logger.info(TAG, "recursos de captura liberados al destruir el servicio")
    }

    /**
     * Recrea el virtual display tras un cambio de configuración (rotación,
     * cambio de resolución o pantalla dividida) para que las capturas sigan
     * el tamaño real de la pantalla.
     */
    fun onDisplayConfigChanged() {
        val projection = mediaProjection ?: return
        if (!lifecycle.isActive) return
        try {
            releaseVirtualDisplay()
            startVirtualDisplay(projection)
            logger.info(TAG, "virtual display recreado tras cambio de configuración")
        } catch (error: Throwable) {
            logger.error(TAG, "error al recrear el virtual display tras cambio de configuración: ${error.message}")
            validationRecorder.record(
                ValidationEvent.CaptureError(
                    System.currentTimeMillis(),
                    "excepción en cambio de configuración: ${error.message}",
                ),
            )
            stopProjection()
        }
    }

    override suspend fun captureFrame(): Bitmap? {
        if (!lifecycle.isActive) return null
        return withContext(Dispatchers.Default) {
            val image =
                withTimeoutOrNull(CAPTURE_TIMEOUT_MS) { frames.receive() }
                    ?: return@withContext null
            try {
                image.toBitmap()
            } catch (error: Throwable) {
                logger.error(TAG, "error convirtiendo frame: ${error.message}")
                null
            } finally {
                image.close()
            }
        }
    }

    private fun startVirtualDisplay(projection: MediaProjection) {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val reader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                MAX_IMAGES,
            )
        reader.setOnImageAvailableListener(
            { reader ->
                if (imageReader !== reader) return@setOnImageAvailableListener
                runCatching {
                    val image = reader.acquireLatestImage()
                    if (image != null) frames.trySend(image)
                }
            },
            mainHandler,
        )
        imageReader = reader

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        virtualDisplay =
            projection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler,
            )
    }

    private fun releaseResources() {
        releaseVirtualDisplay()
        projectionCallback?.let { callback ->
            runCatching { mediaProjection?.unregisterCallback(callback) }
        }
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        projectionCallback = null
    }

    private fun releaseVirtualDisplay() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        drainFrames()
    }

    /** Libera los frames pendientes para no retener memoria tras detener la captura. */
    private fun drainFrames() {
        runCatching {
            while (true) {
                frames.tryReceive().getOrNull()?.close() ?: break
            }
        }
    }

    companion object {
        private const val TAG = "MediaProjectionCapture"
        private const val VIRTUAL_DISPLAY_NAME = "SIRC_CAPTURE"
        private const val MAX_IMAGES = 2
        private const val CAPTURE_TIMEOUT_MS = 400L
    }
}

/**
 * Convierte un [Image] (RGBA_8888) en un [Bitmap] ARGB_8888, respetando el
 * stride del plano para pantallas con padding.
 */
private fun Image.toBitmap(): Bitmap {
    val plane = planes[0]
    val buffer = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val rowPadding = rowStride - pixelStride * width
    buffer.rewind()
    val paddedWidth = width + rowPadding / pixelStride
    val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
    padded.copyPixelsFromBuffer(buffer)
    val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
    if (cropped != padded) {
        padded.recycle()
    }
    return cropped
}
