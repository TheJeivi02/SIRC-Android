package com.sirc.feature.overlay

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sirc.capture.input.CaptureInput
import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.WindowEventType
import com.sirc.capture.scheduler.DebounceCaptureScheduler
import com.sirc.core.platform.CaptureInputType
import com.sirc.core.platform.PlatformDetectionEngine
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.repository.DriverConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    private val driverConfigRepository: DriverConfigRepository,
    private val logger: SircLogger,
) : CaptureInput {
    override val origin: CaptureInputType = CaptureInputType.ACCESSIBILITY

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Plataformas activas para el filtro de captura. Si está vacío, se aceptan
     * todas (comportamiento legado) para no bloquear capturas antes de que el
     * usuario configure plataformas.
     */
    private var activePlatforms: Set<RidePlatform> = emptySet()

    private var lastFingerprint: String = ""

    init {
        scope.launch {
            driverConfigRepository.observeDriverConfig().collect { config ->
                activePlatforms = config?.platforms ?: emptySet()
            }
        }
    }

    /** Fuerza el conjunto de plataformas activas (uso en pruebas deterministas). */
    internal fun setActivePlatforms(platforms: Set<RidePlatform>) {
        activePlatforms = platforms
    }

    /** Plataformas activas actuales (exposición para pruebas). */
    internal val currentActivePlatforms: Set<RidePlatform>
        get() = activePlatforms

    /**
     * Decide si se acepta la captura de [platform]: si no hay plataformas
     * activas configuradas se aceptan todas (comportamiento legado); en caso
     * contrario solo las que el conductor marcó como activas.
     */
    internal fun isPlatformActive(platform: RidePlatform): Boolean =
        activePlatforms.isEmpty() || platform in activePlatforms

    /**
     * Procesa un evento de accesibilidad y, si corresponde, encola un
     * [CaptureRequest] debounced y reenvía el [CaptureWindowEvent] al debug.
     */
    fun onAccessibilityEvent(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
    ) {
        val packageName = event.packageName?.toString()
        if (packageName == null) {
            logger.warn(TAG, "evento sin packageName ignorado")
            return
        }
        val timestampMillis = System.currentTimeMillis()
        logger.debug(TAG, "evento recibido: type=${event.eventType} package=$packageName")
        val detection = detectionEngine.detect(emptyList(), packageName)
        if (!detection.isRecognized) {
            logger.info(TAG, "rechazado: paquete no soportado $packageName")
            return
        }

        val platform =
            detection.descriptor?.platform ?: run {
                logger.warn(TAG, "detección reconocida sin descriptor en $packageName")
                return
            }
        if (!isPlatformActive(platform)) {
            logger.info(TAG, "rechazado: plataforma no activa $platform (package $packageName)")
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            logger.info(TAG, "rechazado: tipo de evento no relevante ${event.eventType} en $packageName")
            return
        }

        val texts = root?.let { collectTexts(it) }
        if (texts == null) {
            logger.info(TAG, "rechazado: sin ventana activa (root null) en $packageName")
            return
        }
        if (texts.isEmpty()) {
            logger.info(TAG, "rechazado: textos vacíos en $packageName")
            return
        }

        val fingerprint = texts.joinToString("|").hashCode().toString()
        if (fingerprint == lastFingerprint) {
            logger.debug(TAG, "rechazado: dedup (mismo fingerprint) en $packageName")
            return
        }
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
        logger.info(TAG, "request programado: package=$packageName textos=${texts.size}")

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
        private const val TAG = "AccessibilityInput"
        private const val MAX_NODES = 400
        private const val MAX_TEXT_LENGTH = 200
        private const val MAX_TEXTS = 80
    }
}
