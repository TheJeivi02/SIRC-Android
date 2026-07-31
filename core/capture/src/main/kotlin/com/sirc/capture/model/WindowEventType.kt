package com.sirc.capture.model

/**
 * Tipos de evento de ventana relevantes para la captura.
 *
 * Valores agnósticos a Android: el servicio de accesibilidad los mapea desde
 * los tipos de [android.view.accessibility.AccessibilityEvent].
 */
enum class WindowEventType {
    WINDOW_STATE_CHANGED,
    WINDOW_CONTENT_CHANGED,
    OTHER,
}
