package com.sirc.feature.overlay

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.WindowEventType
import com.sirc.domain.model.RidePlatform
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Accessibility Service de SIRC.
 *
 * Cumplimiento Google Play:
 *  - Únicamente LEE el contenido visible de las apps de transporte para
 *    reconocer una oferta (monto, distancia, duración).
 *  - NO simula toques, NO acepta/rechaza viajes, NO interactúa con ninguna UI.
 *  - Filtrado por paquetes soportados para minimizar consumo de batería.
 *
 * Reenvía cada cambio de ventana relevante al pipeline de captura
 * ([AccessibilityWindowObserver]) sin ninguna interpretación; la persistencia
 * del historial y el overlay los gestiona el pipeline moderno de captura.
 */
@AndroidEntryPoint
class SircAccessibilityService : AccessibilityService() {
    @Inject lateinit var windowObserver: AccessibilityWindowObserver

    private var lastFingerprint: String = ""

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

        windowObserver.onWindowEvent(
            CaptureWindowEvent(
                eventId = System.nanoTime(),
                packageName = packageName,
                eventType = event.eventType.toWindowEventType(),
                timestampMillis = System.currentTimeMillis(),
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
