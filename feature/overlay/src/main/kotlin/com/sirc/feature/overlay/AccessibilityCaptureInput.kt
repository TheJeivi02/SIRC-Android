package com.sirc.feature.overlay

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sirc.capture.input.CaptureInput
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.WindowEventType
import com.sirc.capture.scheduler.DebounceCaptureScheduler
import com.sirc.core.platform.CaptureInputType
import com.sirc.core.platform.PlatformDetectionEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entrada de captura por accesibilidad (WP-E3-03).
 *
 * Extrae la lógica que vivía en [CaptureAccessibilityService]: filtro por
 * plataforma, filtro de tipos de evento, [collectTexts] con los límites duros
 * 400 nodos / 80 textos / ≤200 chars, dedup por fingerprint, armado del
 * [CaptureRequest] con `origin = ACCESSIBILITY`, encolado en el
 * [DebounceCaptureScheduler] y reenvío del [CaptureWindowEvent] al
 * [WindowEventPublisher] para el panel de depuración (sin duplicación).
 *
 * No conoce la UI ni el pipeline: solo genera solicitudes.
 *
 * La resolución de plataforma por paquete se delega en [PlatformDetectionEngine]
 * (única fuente de verdad, igual que el pipeline, WP-E3-05A).
 */
@Singleton
class AccessibilityCaptureInput @Inject constructor(
    private val scheduler: DebounceCaptureScheduler,
    private val windowEventPublisher: WindowEventPublisher,
    private val detectionEngine: PlatformDetectionEngine,
) : CaptureInput {
    override val origin: CaptureInputType = CaptureInputType.ACCESSIBILITY

    private var lastFingerprint: String = ""

    /**
     * Procesa un evento de accesibilidad y, si corresponde, encola un
     * [CaptureRequest] debounced y reenvía el [CaptureWindowEvent] al debug.
     */
    fun onAccessibilityEvent(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
    ) {
        val packageName = event.packageName?.toString() ?: return
        val timestampMillis = System.currentTimeMillis()
        if (!detectionEngine.detect(emptyList(), timestampMillis, packageName).isRecognized) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val texts = root?.let { collectTexts(it) } ?: return
        if (texts.isEmpty()) return

        val fingerprint = texts.joinToString("|").hashCode().toString()
        if (fingerprint == lastFingerprint) return
        lastFingerprint = fingerprint

        val request =
            CaptureRequest(
                id = System.nanoTime(),
                packageName = packageName,
                timestampMillis = timestampMillis,
                texts = texts,
                origin = CaptureInputType.ACCESSIBILITY,
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

    override fun requests(): Flow<CaptureRequest> = scheduler.debouncedRequests()

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
