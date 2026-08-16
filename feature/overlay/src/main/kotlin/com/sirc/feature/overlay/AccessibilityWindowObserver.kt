package com.sirc.feature.overlay

import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.observer.WindowObserver
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observador de ventanas que recibe cambios de
 * [CaptureAccessibilityService] y los emite como Flow para el
 * [com.sirc.capture.coordinator.OfferCaptureCoordinator].
 *
 * Solo observa: nunca ejecuta acciones sobre la interfaz.
 */
@Singleton
class AccessibilityWindowObserver @Inject constructor() : WindowObserver, WindowEventPublisher {
    private val _windowEvents =
        MutableSharedFlow<CaptureWindowEvent>(
            extraBufferCapacity = BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override val windowEvents: SharedFlow<CaptureWindowEvent> = _windowEvents.asSharedFlow()

    override fun onWindowEvent(event: CaptureWindowEvent) {
        _windowEvents.tryEmit(event)
    }

    companion object {
        private const val BUFFER_CAPACITY = 64
    }
}

/** Contrato para publicar eventos de ventana desde un servicio de accesibilidad. */
interface WindowEventPublisher {
    fun onWindowEvent(event: CaptureWindowEvent)
}
