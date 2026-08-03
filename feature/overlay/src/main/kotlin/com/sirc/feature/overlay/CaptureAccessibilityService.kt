package com.sirc.feature.overlay

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.WindowEventType
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.capture.scheduler.DebounceCaptureScheduler
import com.sirc.domain.model.RidePlatform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility Service dedicado a la captura de ofertas.
 *
 * Único servicio de accesibilidad registrado en producción (WP-E1-03:
 * `SircAccessibilityService` fue eliminado).
 *
 * Está completamente desacoplado de la UI: no publica en el overlay ni conoce
 * estados de la interfaz. Observa los cambios de ventana de las plataformas
 * soportadas, construye [CaptureRequest] y los encola en el
 * [DebounceCaptureScheduler]; el pipeline recibe únicamente el último request
 * tras un periodo de silencio (evita ejecutar OCR en cada evento).
 *
 * También reenvía eventos como [CaptureWindowEvent] al [WindowEventPublisher] para
 * preservar la observación en el panel de depuración (WP-E1-03: la
 * `AccessibilityWindowObserver` legacy fue integrada aquí sin duplicación).
 */
@AndroidEntryPoint
class CaptureAccessibilityService : AccessibilityService() {
    @Inject lateinit var pipeline: CapturePipeline

    @Inject lateinit var scheduler: DebounceCaptureScheduler

    @Inject lateinit var windowEventPublisher: WindowEventPublisher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastFingerprint: String = ""

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            scheduler.debouncedRequests()
                .collect { request -> pipeline.process(request) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (RidePlatform.fromPackageName(packageName) == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val root = rootInActiveWindow ?: return
        val texts = collectTexts(root)
        if (texts.isEmpty()) return

        val fingerprint = texts.joinToString("|").hashCode().toString()
        if (fingerprint == lastFingerprint) return
        lastFingerprint = fingerprint

        val timestampMillis = System.currentTimeMillis()

        val request =
            CaptureRequest(
                id = System.nanoTime(),
                packageName = packageName,
                timestampMillis = timestampMillis,
                texts = texts,
            )
        scheduler.schedule(request)

        windowEventPublisher.onWindowEvent(
            CaptureWindowEvent(
                eventId = request.id,
                packageName = packageName,
                eventType = event.eventType.toWindowEventType(),
                timestampMillis = timestampMillis,
                textCount = texts.size,
                fingerprint = fingerprint,
                texts = texts,
            ),
        )
    }

    /**
     * Recorre el árbol de accesibilidad con límites duros de nodos y texto
     * para garantizar un costo mínimo de memoria y batería.
     */
    private fun collectTexts(root: AccessibilityNodeInfo): List<String> {
        val result = mutableListOf<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0

        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val node = stack.removeLast()
            visited++

            node.text?.toString()?.trim()?.let { text ->
                if (text.isNotEmpty() && text.length <= MAX_TEXT_LENGTH) result += text
            }
            node.contentDescription?.toString()?.trim()?.let { text ->
                if (text.isNotEmpty() && text.length <= MAX_TEXT_LENGTH) result += text
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.addLast(it) }
            }
        }
        return result.take(MAX_TEXTS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun Int.toWindowEventType(): WindowEventType =
        when (this) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> WindowEventType.WINDOW_STATE_CHANGED
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> WindowEventType.WINDOW_CONTENT_CHANGED
            else -> WindowEventType.OTHER
        }

    companion object {
        private const val MAX_NODES = 400
        private const val MAX_TEXT_LENGTH = 200
        private const val MAX_TEXTS = 80
    }
}
