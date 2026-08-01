package com.sirc.domain.session

/** Ciclo de vida de una sesión de captura. */
enum class SessionStatus {
    /** Sin sesión activa: no se está capturando. */
    IDLE,

    /** Capturando y analizando ofertas. */
    ACTIVE,

    /** Sesión iniciada pero detenida temporalmente por el conductor. */
    PAUSED,
}
