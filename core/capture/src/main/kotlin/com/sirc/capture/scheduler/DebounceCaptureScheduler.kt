package com.sirc.capture.scheduler

import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.CaptureRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler de captura con debounce.
 *
 * Coalesce los [CaptureRequest] que llegan de los eventos de accesibilidad
 * (muy frecuentes) y emite solo el último tras un periodo de silencio, de modo
 * que el OCR no se ejecute en cada `AccessibilityEvent`.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@Singleton
class DebounceCaptureScheduler @Inject constructor(
    private val logger: SircLogger,
) {
    private val requests =
        MutableSharedFlow<CaptureRequest>(
            extraBufferCapacity = BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /** Registra un request pendiente; el último tras el debounce se emite. */
    fun schedule(request: CaptureRequest) {
        logger.debug(TAG, "request encolado: id=${request.id} package=${request.packageName}")
        requests.tryEmit(request)
    }

    /** Flujo de requests ya debounced, listo para conectar con el pipeline. */
    fun debouncedRequests(debounceMillis: Long = DEFAULT_DEBOUNCE_MS): Flow<CaptureRequest> =
        requests
            .debounce(debounceMillis)
            .onEach {
                logger.info(
                    TAG,
                    "request emitido tras debounce: id=${it.id} package=${it.packageName} " +
                        "textos=${it.texts.size}",
                )
            }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 400L
        private const val BUFFER_CAPACITY = 64
        private const val TAG = "DebounceCaptureScheduler"
    }
}
