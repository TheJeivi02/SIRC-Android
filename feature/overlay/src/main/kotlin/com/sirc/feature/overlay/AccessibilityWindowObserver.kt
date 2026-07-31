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
 * Observador de ventanas que recibe los cambios del
 * [SircAccessibilityService] y los emite como Flow para el pipeline de
 * captura. Solo observa: nunca ejecuta acciones sobre la interfaz.
 */
@Singleton
class AccessibilityWindowObserver @Inject constructor() : WindowObserver {
    private val _windowEvents =
        MutableSharedFlow<CaptureWindowEvent>(
            extraBufferCapacity = BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override val windowEvents: SharedFlow<CaptureWindowEvent> = _windowEvents.asSharedFlow()

    fun onWindowEvent(event: CaptureWindowEvent) {
        _windowEvents.tryEmit(event)
    }

    companion object {
        private const val BUFFER_CAPACITY = 64
    }
}
