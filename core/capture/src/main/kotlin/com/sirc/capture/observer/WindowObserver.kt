package com.sirc.capture.observer

import com.sirc.capture.model.CaptureWindowEvent
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de eventos de ventana para el pipeline de captura.
 *
 * El servicio de accesibilidad publica los cambios observados; el
 * [com.sirc.capture.coordinator.OfferCaptureCoordinator] los consume.
 */
interface WindowObserver {
    val windowEvents: Flow<CaptureWindowEvent>
}
