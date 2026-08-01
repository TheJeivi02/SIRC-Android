package com.sirc.domain.session

import com.sirc.domain.model.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureSessionManagerTest {
    private val manager = CaptureSessionManager()
    private var now = 0L

    init {
        manager.setClockForTesting { now }
    }

    @Test
    fun `inicio activa la sesion`() {
        manager.start()

        assertEquals(SessionStatus.ACTIVE, manager.stats.value.status)
        assertEquals(0L, manager.stats.value.activeSeconds)
    }

    @Test
    fun `pause acumula la duracion activa`() {
        manager.start()
        now = 5_000
        manager.pause()

        assertEquals(SessionStatus.PAUSED, manager.stats.value.status)
        assertEquals(5L, manager.stats.value.activeSeconds)
    }

    @Test
    fun `resume y stop acumulan la duracion total`() {
        manager.start()
        now = 5_000
        manager.pause()
        now = 10_000
        manager.resume()
        now = 14_000
        manager.stop()

        assertEquals(SessionStatus.IDLE, manager.stats.value.status)
        assertEquals(9L, manager.stats.value.activeSeconds)
    }

    @Test
    fun `start sobre una sesion existente no la reinicia`() {
        manager.start()
        manager.recordOffer(Decision.PROFITABLE)
        manager.start()

        assertEquals(1, manager.stats.value.offersProcessed)
        assertEquals(1, manager.stats.value.offersAccepted)
    }

    @Test
    fun `recordOffer cuenta procesadas aceptadas y rechazadas`() {
        manager.start()
        manager.recordOffer(Decision.PROFITABLE)
        manager.recordOffer(Decision.NOT_PROFITABLE)
        manager.recordOffer(Decision.MARGINAL)
        manager.recordOffer(null)

        assertEquals(4, manager.stats.value.offersProcessed)
        assertEquals(1, manager.stats.value.offersAccepted)
        assertEquals(1, manager.stats.value.offersRejected)
    }

    @Test
    fun `recordError incrementa el contador de errores`() {
        manager.start()
        manager.recordError()
        manager.recordError()

        assertEquals(2, manager.stats.value.errors)
    }

    @Test
    fun `reset borra la sesion`() {
        manager.start()
        manager.recordOffer(Decision.PROFITABLE)
        manager.recordError()

        manager.reset()

        assertEquals(SessionStatus.IDLE, manager.stats.value.status)
        assertEquals(0, manager.stats.value.offersProcessed)
        assertEquals(0, manager.stats.value.errors)
        assertNull(manager.stats.value.sessionStartedAtMillis)
    }
}
