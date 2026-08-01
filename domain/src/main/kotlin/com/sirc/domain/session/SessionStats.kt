package com.sirc.domain.session

/**
 * Estadísticas acumuladas de la sesión de captura en curso.
 *
 * [activeSeconds] se calcula en vivo: la duración activa acumulada más el
 * tramo en curso cuando la sesión está [SessionStatus.ACTIVE]. El reloj
 * ([clock]) es inyectable para hacer deterministas las pruebas; no participa
 * en la igualdad estructural del data class.
 */
data class SessionStats(
    val status: SessionStatus = SessionStatus.IDLE,
    val sessionStartedAtMillis: Long? = null,
    val resumedAtMillis: Long? = null,
    val activeAccumulatedMillis: Long = 0L,
    val offersProcessed: Int = 0,
    val offersAccepted: Int = 0,
    val offersRejected: Int = 0,
    val errors: Int = 0,
    private val clock: (() -> Long)? = null,
) {
    val activeSeconds: Long
        get() {
            val resumed = resumedAtMillis
            val running = if (resumed != null) (clock ?: System::currentTimeMillis)() - resumed else 0L
            return (activeAccumulatedMillis + running) / MILLIS_PER_SECOND
        }

    override fun equals(other: Any?): Boolean =
        other is SessionStats &&
            status == other.status &&
            sessionStartedAtMillis == other.sessionStartedAtMillis &&
            resumedAtMillis == other.resumedAtMillis &&
            activeAccumulatedMillis == other.activeAccumulatedMillis &&
            offersProcessed == other.offersProcessed &&
            offersAccepted == other.offersAccepted &&
            offersRejected == other.offersRejected &&
            errors == other.errors

    override fun hashCode(): Int =
        listOf(
            status,
            sessionStartedAtMillis,
            resumedAtMillis,
            activeAccumulatedMillis,
            offersProcessed,
            offersAccepted,
            offersRejected,
            errors,
        ).hashCode()

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
