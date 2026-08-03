package com.sirc.capture.coordinator

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.InMemoryFeatureFlags
import com.sirc.capture.log.TestLogger
import com.sirc.capture.model.CaptureSessionStatus
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.SnapshotSource
import com.sirc.capture.model.WindowEventType
import com.sirc.capture.observer.WindowObserver
import com.sirc.capture.parser.OfferParser
import com.sirc.capture.repository.InMemoryCaptureRepository
import com.sirc.domain.model.RidePlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferCaptureCoordinatorTest {
    private val observer = FakeWindowObserver()
    private val parser = FakeOfferParser()
    private val repository = InMemoryCaptureRepository()
    private val featureFlags = InMemoryFeatureFlags()
    private val logger = TestLogger()

    private val coordinator =
        OfferCaptureCoordinator(
            windowObserver = observer,
            parser = parser,
            captureRepository = repository,
            featureFlags = featureFlags,
            logger = logger,
        )

    @Test
    fun `evento de plataforma abre sesión y guarda snapshot`() =
        runBlocking {
            coordinator.process(eventFor(RidePlatform.UBER.packageName))

            val state = coordinator.state.value
            assertNotNull(state.activeSession)
            assertEquals(RidePlatform.UBER.packageName, state.activeSession?.packageName)
            assertEquals(CaptureSessionStatus.ACTIVE, state.activeSession?.status)
            assertEquals(1, state.activeSession?.capturedSnapshotCount)
            assertEquals(RidePlatform.UBER, state.lastSnapshot?.platform)
            assertEquals(1, state.eventsProcessed)
            assertNotNull(state.lastProcessingTimeMillis)
            assertEquals(1, repository.snapshots().size)
        }

    @Test
    fun `reutiliza la sesión activa de la misma plataforma`() =
        runBlocking {
            coordinator.process(eventFor(RidePlatform.UBER.packageName))
            val firstSessionId = coordinator.state.value.activeSession?.id

            coordinator.process(eventFor(RidePlatform.UBER.packageName))

            val state = coordinator.state.value
            assertEquals(firstSessionId, state.activeSession?.id)
            assertEquals(2, state.activeSession?.capturedSnapshotCount)
            assertEquals(2, state.eventsProcessed)
        }

    @Test
    fun `cambiar de plataforma abre una nueva sesión`() =
        runBlocking {
            coordinator.process(eventFor(RidePlatform.UBER.packageName))

            coordinator.process(eventFor(RidePlatform.DIDI.packageName))

            val state = coordinator.state.value
            assertEquals(RidePlatform.DIDI.packageName, state.activeSession?.packageName)
            assertEquals(RidePlatform.DIDI, state.lastSnapshot?.platform)
        }

    @Test
    fun `paquete no soportado cierra la sesión activa`() =
        runBlocking {
            coordinator.process(eventFor(RidePlatform.UBER.packageName))

            coordinator.process(eventFor("com.unknown.app"))

            val state = coordinator.state.value
            assertEquals(CaptureSessionStatus.CLOSED, state.activeSession?.status)
            assertEquals(2, state.eventsProcessed)
        }

    @Test
    fun `flag CAPTURE desactivada ignora los eventos`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.CAPTURE, false)

            coordinator.process(eventFor(RidePlatform.UBER.packageName))

            val state = coordinator.state.value
            assertNull(state.activeSession)
            assertNull(state.lastSnapshot)
            assertEquals(0, state.eventsProcessed)
            assertEquals(0, repository.snapshots().size)
        }

    @Test
    fun `flag PARSER desactivada no genera snapshot`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.PARSER, false)

            coordinator.process(eventFor(RidePlatform.UBER.packageName))

            val state = coordinator.state.value
            assertNotNull(state.activeSession)
            assertNull(state.lastSnapshot)
            assertEquals(1, state.eventsProcessed)
            assertEquals(0, repository.snapshots().size)
        }

    @Test
    fun `reset limpia estado y repositorio`() =
        runBlocking {
            coordinator.process(eventFor(RidePlatform.UBER.packageName))
            assertEquals(1, repository.snapshots().size)

            coordinator.reset()

            assertNull(coordinator.state.value.lastSnapshot)
            assertNull(coordinator.state.value.activeSession)
            assertEquals(0, coordinator.state.value.eventsProcessed)
            assertEquals(0, repository.snapshots().size)
        }

    @Test
    fun `start y stop cambian isCapturing`() =
        runBlocking {
            assertFalse(coordinator.state.value.isCapturing)

            coordinator.start()
            assertEquals(true, coordinator.state.value.isCapturing)

            coordinator.stop()
            assertFalse(coordinator.state.value.isCapturing)
        }

    private fun eventFor(packageName: String): CaptureWindowEvent =
        CaptureWindowEvent(
            eventId = System.nanoTime(),
            packageName = packageName,
            eventType = WindowEventType.WINDOW_STATE_CHANGED,
            timestampMillis = System.currentTimeMillis(),
            textCount = 5,
            fingerprint = "fp-$packageName",
        )

    private class FakeWindowObserver : WindowObserver {
        private val flow = MutableSharedFlow<CaptureWindowEvent>(extraBufferCapacity = 16)

        override val windowEvents: Flow<CaptureWindowEvent> = flow

        fun emit(event: CaptureWindowEvent) = flow.tryEmit(event)
    }

    private class FakeOfferParser : OfferParser {
        override fun parse(
            event: CaptureWindowEvent,
            session: OfferCaptureSession,
        ): OfferSnapshot? {
            val platform = RidePlatform.fromPackageName(event.packageName) ?: return null
            return OfferSnapshot(
                sessionId = session.id,
                platform = platform,
                capturedAtMillis = event.timestampMillis,
                source = SnapshotSource.REAL,
                estimatedTotal = 125.0,
                distanceKm = 8.5,
                durationMin = 22.0,
                rawData = "fake-test",
                texts = event.texts,
            )
        }
    }
}
