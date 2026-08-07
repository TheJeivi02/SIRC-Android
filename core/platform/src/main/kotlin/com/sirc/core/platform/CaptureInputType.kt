package com.sirc.core.platform

/**
 * Tipo de entrada de captura.
 *
 * Identifica la fuente de una captura (accesibilidad, MediaProjection).
 * Los valores legacy (PACKAGE/OCR/UNKNOWN) se conservan por compatibilidad
 * con el framework de detección; los nuevos describen las entradas de
 * captura reales.
 */
enum class CaptureInputType {
    /** Textos etiquetados con el package de origen. */
    PACKAGE,

    /** Textos provenientes de OCR en tiempo real. */
    OCR,

    /** Entrada no informada. */
    UNKNOWN,

    /** Eventos de accesibilidad (service). */
    ACCESSIBILITY,

    /** Captura por proyección de pantalla (MediaProjection). */
    MEDIA_PROJECTION,
}
