package com.sirc.capture.parser

import com.sirc.capture.model.CaptureSessionStatus
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.SnapshotSource
import com.sirc.capture.model.WindowEventType
import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FakeParserTest {
    private val parser = FakeParser()

    @Test
    fun `genera snapshot simulado para una plataforma soportada`() {
        val event = eventFor(RidePlatform.UBER.packageName)
        val session = sessionFor(RidePlatform.UBER.packageName)

        val snapshot = parser.parse(event, session)

        assertNotNull(snapshot)
        snapshot?.let {
            assertEquals(RidePlatform.UBER, it.platform)
            assertEquals(SnapshotSource.FAKE, it.source)
            assertEquals(session.id, it.sessionId)
            assertEquals(FakeParser.FAKE_ESTIMATED_TOTAL, it.estimatedTotal, 0.0)
            assertEquals(FakeParser.FAKE_DISTANCE_KM, it.distanceKm, 0.0)
            assertEquals(FakeParser.FAKE_DURATION_MIN, it.durationMin, 0.0)
        }
    }

    @Test
    fun `devuelve null para un paquete no soportado`() {
        val event = eventFor("com.unknown.app")
        val session = sessionFor("com.unknown.app")

        assertNull(parser.parse(event, session))
    }

    private fun eventFor(packageName: String): CaptureWindowEvent =
        CaptureWindowEvent(
            eventId = 1,
            packageName = packageName,
            eventType = WindowEventType.WINDOW_STATE_CHANGED,
            timestampMillis = 1_000,
            textCount = 5,
            fingerprint = "fp",
        )

    private fun sessionFor(packageName: String): OfferCaptureSession =
        OfferCaptureSession(
            id = "session-1",
            startedAtMillis = 1_000,
            packageName = packageName,
            status = CaptureSessionStatus.ACTIVE,
        )
}
