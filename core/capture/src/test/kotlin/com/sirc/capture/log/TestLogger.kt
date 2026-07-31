package com.sirc.capture.log

/** Logger que acumula mensajes en memoria para las pruebas. */
class TestLogger : SircLogger {
    val messages = mutableListOf<String>()

    override fun debug(
        tag: String,
        message: String,
    ) {
        messages += "D $tag: $message"
    }

    override fun info(
        tag: String,
        message: String,
    ) {
        messages += "I $tag: $message"
    }

    override fun warn(
        tag: String,
        message: String,
    ) {
        messages += "W $tag: $message"
    }

    override fun error(
        tag: String,
        message: String,
    ) {
        messages += "E $tag: $message"
    }

    fun clear() = messages.clear()
}
