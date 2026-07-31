package com.sirc.capture.log

/**
 * Sistema centralizado de logs.
 *
 * La implementación concreta decide si emite o no (p. ej. solo en build de
 * desarrollo); el dominio de captura nunca loguea por su cuenta.
 */
interface SircLogger {
    fun debug(
        tag: String,
        message: String,
    )

    fun info(
        tag: String,
        message: String,
    )

    fun warn(
        tag: String,
        message: String,
    )

    fun error(
        tag: String,
        message: String,
    )
}
