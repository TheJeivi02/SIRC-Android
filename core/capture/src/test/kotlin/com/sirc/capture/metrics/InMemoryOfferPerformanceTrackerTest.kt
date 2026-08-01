package com.sirc.capture.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryOfferPerformanceTrackerTest {
    private val tracker = InMemoryOfferPerformanceTracker()

    @Test
    fun `record agrega una oferta y expone su timing`() {
        tracker.record(OfferTiming(captureMillis = 5.0, parseMillis = 3.0, totalMillis = 12.0))

        assertEquals(1, tracker.lastOffers.value.size)
        val timing = tracker.lastOffers.value.single()
        assertEquals(5.0, timing.captureMillis ?: 0.0, 0.001)
        assertEquals(12.0, timing.totalMillis ?: 0.0, 0.001)
        assertNull(timing.evaluationMillis)
    }

    @Test
    fun `merge completa la última oferta sin tiempos de overlay`() {
        tracker.record(OfferTiming(captureMillis = 5.0, parseMillis = 3.0, totalMillis = 12.0))
        tracker.merge(OfferTiming(evaluationMillis = 4.0, overlayMillis = 1.0))

        val timing = tracker.lastOffers.value.single()
        assertEquals(4.0, timing.evaluationMillis ?: 0.0, 0.001)
        assertEquals(1.0, timing.overlayMillis ?: 0.0, 0.001)
        assertEquals(5.0, timing.captureMillis ?: 0.0, 0.001)
    }

    @Test
    fun `merge sin ofertas previas crea un registro parcial`() {
        tracker.merge(OfferTiming(evaluationMillis = 4.0, overlayMillis = 1.0))

        val timing = tracker.lastOffers.value.single()
        assertEquals(4.0, timing.evaluationMillis ?: 0.0, 0.001)
        assertNull(timing.captureMillis)
    }

    @Test
    fun `promedio considera solo la ventana de las últimas 20 ofertas`() {
        repeat(25) { index ->
            tracker.record(OfferTiming(captureMillis = (index + 1).toDouble(), totalMillis = 100.0))
        }

        val averages = tracker.averages.value
        // La ventana cubre la oferta 6..25: (6+7+...+25)/20 = 15.5
        assertEquals(15.5, averages.captureMillis ?: 0.0, 0.001)
        assertEquals(100.0, averages.totalMillis ?: 0.0, 0.001)
    }

    @Test
    fun `clear vacía el historial y los promedios`() {
        tracker.record(OfferTiming(captureMillis = 5.0))

        tracker.clear()

        assertEquals(0, tracker.lastOffers.value.size)
        assertNull(tracker.averages.value.captureMillis)
    }

    @Test
    fun `conserva a lo sumo 100 ofertas`() {
        repeat(150) {
            tracker.record(OfferTiming(totalMillis = 1.0))
        }

        assertEquals(InMemoryOfferPerformanceTracker.MAX_OFFERS, tracker.lastOffers.value.size)
        assertNotNull(tracker.lastOffers.value.first())
    }
}
