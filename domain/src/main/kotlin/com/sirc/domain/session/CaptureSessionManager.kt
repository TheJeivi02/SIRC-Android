package com.sirc.domain.session

import com.sirc.domain.model.Decision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de la sesión de captura (O1).
 *
 * Controla el ciclo de vida de una sesión (iniciar, pausar, reanudar, detener)
 * y acumula las estadísticas de la sesión: duración activa, ofertas
 * procesadas/aceptadas/rechazadas y errores. Es una máquina de estados pura
 * (sin dependencias de Android) expuesta como [stats].
 */
@Singleton
class CaptureSessionManager @Inject constructor() {
    private val _stats = MutableStateFlow(SessionStats())
    val stats: StateFlow<SessionStats> = _stats.asStateFlow()

    private var clock: () -> Long = System::currentTimeMillis

    /** Inicia una sesión nueva (no-op si ya hay una activa o pausada). */
    @Synchronized
    fun start() {
        val current = _stats.value
        if (current.status != SessionStatus.IDLE) return
        val now = clock()
        _stats.value =
            SessionStats(
                status = SessionStatus.ACTIVE,
                sessionStartedAtMillis = now,
                resumedAtMillis = now,
                clock = clock,
            )
    }

    /** Pausa la sesión sin perder lo acumulado. */
    @Synchronized
    fun pause() {
        val current = _stats.value
        if (current.status != SessionStatus.ACTIVE) return
        val now = clock()
        val running = current.resumedAtMillis?.let { now - it } ?: 0L
        _stats.value =
            current.copy(
                status = SessionStatus.PAUSED,
                resumedAtMillis = null,
                activeAccumulatedMillis = current.activeAccumulatedMillis + running,
            )
    }

    /** Reanuda una sesión pausada. */
    @Synchronized
    fun resume() {
        val current = _stats.value
        if (current.status != SessionStatus.PAUSED) return
        _stats.value = current.copy(status = SessionStatus.ACTIVE, resumedAtMillis = clock())
    }

    /** Detiene la sesión conservando las estadísticas finales. */
    @Synchronized
    fun stop() {
        val current = _stats.value
        if (current.status == SessionStatus.IDLE) return
        pause()
        _stats.value = _stats.value.copy(status = SessionStatus.IDLE)
    }

    /** Borra la sesión y sus estadísticas. */
    @Synchronized
    fun reset() {
        _stats.value = SessionStats()
    }

    /** Registra una oferta procesada y su decisión (aceptada/rechazada). */
    @Synchronized
    fun recordOffer(decision: Decision?) {
        val current = _stats.value
        _stats.value =
            current.copy(
                offersProcessed = current.offersProcessed + 1,
                offersAccepted = current.offersAccepted + if (decision == Decision.PROFITABLE) 1 else 0,
                offersRejected = current.offersRejected + if (decision == Decision.NOT_PROFITABLE) 1 else 0,
            )
    }

    /** Registra un error de captura o de análisis. */
    @Synchronized
    fun recordError() {
        _stats.value = _stats.value.copy(errors = _stats.value.errors + 1)
    }

    /** Permite inyectar un reloj determinista en pruebas. */
    @Synchronized
    internal fun setClockForTesting(clock: () -> Long) {
        this.clock = clock
    }
}
