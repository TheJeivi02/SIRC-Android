package com.sirc.capture.android.provider

/**
 * Máquina de estados determinista y única fuente de verdad para el ciclo de vida
 * de la captura de pantalla (`MediaProjectionScreenCaptureProvider`).
 *
 * Estados gestionados:
 * - [State.IDLE]: Sin recursos adquiridos ni proyección activa.
 * - [State.INITIALIZING]: Transitorio durante la obtención del token y creación de superficies.
 * - [State.ACTIVE]: Sesión de captura totalmente activa y operativa.
 *
 * El mecanismo de [generation token] (generaciones incrementales) garantiza que
 * callbacks asíncronos tardíos o eventos de parada pertenecientes a una sesión
 * anterior sean ignorados y no afecten una sesión nueva después de una reinicialización.
 */
internal class ProjectionLifecycle {
    private var state = State.IDLE
    private var generation = 0L

    internal enum class State {
        IDLE,
        INITIALIZING,
        ACTIVE,
    }

    val isActive: Boolean
        get() = state == State.ACTIVE

    val currentState: State
        get() = state

    /**
     * Inicia una nueva sesión de inicialización. Incrementa el generation token,
     * invalidando automáticamente cualquier callback o intento de activación pendiente
     * de sesiones anteriores.
     */
    fun begin(): Long {
        generation++
        state = State.INITIALIZING
        return generation
    }

    /**
     * Pasa el estado a [State.ACTIVE] solo si el token provisto coincide con la
     * generación actual. Si el token es obsoleto (pertenece a una sesión anterior
     * que ya fue reiniciada o detenida), la activación es rechazada.
     */
    fun activate(token: Long): Boolean {
        if (token != generation || state != State.INITIALIZING) return false
        state = State.ACTIVE
        return true
    }

    /**
     * Aborta la inicialización en curso y regresa a [State.IDLE], incrementando
     * la generación para invalidar cualquier operación pendiente.
     */
    fun abort(token: Long) {
        if (token == generation) {
            generation++
            state = State.IDLE
        }
    }

    /**
     * Detiene la sesión activa o en inicialización, regresando a [State.IDLE]
     * e invalidando el token actual. Idempotente.
     */
    fun stop() {
        generation++
        state = State.IDLE
    }

    /**
     * Valida si el token provisto pertenece estrictamente a la sesión vigente.
     */
    fun isCurrent(token: Long): Boolean = token == generation && state != State.IDLE
}
