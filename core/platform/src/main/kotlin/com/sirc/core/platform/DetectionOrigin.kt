package com.sirc.core.platform

/**
 * Procedencia de los textos analizados.
 *
 * No modifica el comportamiento actual; deja preparado el framework para
 * capturas desde galería, pruebas internas y futuros laboratorios de
 * detección.
 */
enum class DetectionOrigin {
    /** Textos etiquetados con el package de origen. */
    PACKAGE,

    /** Textos provenientes de OCR en tiempo real. */
    OCR,

    /** Captura almacenada (p. ej. galería). */
    GALLERY,

    /** Textos de prueba unitaria. */
    TEST,

    /** Origen no informado. */
    UNKNOWN,
}
