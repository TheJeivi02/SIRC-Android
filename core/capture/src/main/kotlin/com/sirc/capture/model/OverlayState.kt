package com.sirc.capture.model

/**
 * Estados mínimos del ciclo de vida del overlay/captura.
 *
 * - [DISABLED]: captura apagada.
 * - [WAITING]: esperando una oferta en pantalla.
 * - [CAPTURING]: se capturó el contenido de la pantalla.
 * - [PROCESSING]: se está aplicando OCR o parseo.
 * - [ERROR]: el pipeline falló.
 */
enum class OverlayState {
    DISABLED,
    WAITING,
    CAPTURING,
    PROCESSING,
    ERROR,
}
