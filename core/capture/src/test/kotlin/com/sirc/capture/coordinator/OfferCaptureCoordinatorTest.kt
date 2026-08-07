package com.sirc.capture.coordinator

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.InMemoryFeatureFlags
import com.sirc.capture.log.TestLogger
import com.sirc.capture.metrics.ProcessingMetrics
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.CaptureSessionStatus
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.model.SnapshotSource
import com.sirc.capture.observer.WindowObserver
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.capture.repository.InMemoryCaptureRepository
import com.sirc.core.platform.PlatformDescriptorRegistry
import com.sirc.core.platform.PlatformDescriptors
import com.sirc.core.platform.PlatformDetectionEngine
import com.sirc.domain.model.RidePlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferCaptureCoordinatorTest {
    private val observer = FakeWindowObserver()
    private val pipeline = FakePipeline()
    private val repository = InMemoryCaptureRepository()
    private val featureFlags = InMemoryFeatureFlags()
    private val logger = TestLogger()

    private val coordinator =
        OfferCaptureCoordinator(
            windowObserver = observer,
            pipeline = pipeline,
            captureRepository = repository,
            featureFlags = featureFlags,
            logger = logger,
            detectionEngine =
                PlatformDetectionEngine(
                    PlatformDescriptorRegistry(PlatformDescriptors.all()),
                ),
        )

    @Test
    fun `evento de plataforma abre sesión y guarda snapshot`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            val state = coordinator.state.value
            assertNotNull(state.activeSession)
            assertEquals(RidePlatform.UBER.packageName, state.activeSession?.packageName)
            assertEquals(CaptureSessionStatus.ACTIVE, state.activeSession?.status)

            val snapshot =
                OfferSnapshot(
                    sessionId = "pipeline-123",
                    platform = RidePlatform.UBER,
                    capturedAtMillis = System.currentTimeMillis(),
                    source = SnapshotSource.REAL,
                    estimatedTotal = 125.0,
                    distanceKm = 8.5,
                    durationMin = 22.0,
                    detectionMillis = 50.0,
                )
            coordinator.onSnapshot(snapshot)

            val updatedState = coordinator.state.value
            assertEquals(1, updatedState.activeSession?.capturedSnapshotCount)
            assertEquals(RidePlatform.UBER, updatedState.lastSnapshot?.platform)
            assertEquals(1, updatedState.eventsProcessed)
            assertNotNull(updatedState.lastProcessingTimeMillis)
        }

    @Test
    fun `reutiliza la sesión activa de la misma plataforma`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))
            val firstSessionId = coordinator.state.value.activeSession?.id

            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            val state = coordinator.state.value
            assertEquals(firstSessionId, state.activeSession?.id)
            assertEquals(2, state.eventsProcessed)
        }

    @Test
    fun `cambiar de plataforma abre una nueva sesión`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            val snapshot1 =
                OfferSnapshot(
                    sessionId = "pipeline-1",
                    platform = RidePlatform.UBER,
                    capturedAtMillis = System.currentTimeMillis(),
                    source = SnapshotSource.REAL,
                    estimatedTotal = 125.0,
                    distanceKm = 8.5,
                    durationMin = 22.0,
                    detectionMillis = 50.0,
                )
            coordinator.onSnapshot(snapshot1)

            coordinator.onWindowEvent(eventFor(RidePlatform.DIDI.packageName))

            val state = coordinator.state.value
            assertEquals(RidePlatform.DIDI.packageName, state.activeSession?.packageName)
        }

    @Test
    fun `paquete no soportado cierra la sesión activa`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            coordinator.onWindowEvent(eventFor("com.unknown.app"))

            val state = coordinator.state.value
            assertEquals(CaptureSessionStatus.CLOSED, state.activeSession?.status)
            assertEquals(2, state.eventsProcessed)
        }

    @Test
    fun `flag CAPTURE desactivada ignora los eventos`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.CAPTURE, false)

            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            val state = coordinator.state.value
            assertNull(state.activeSession)
            assertNull(state.lastSnapshot)
            assertEquals(0, state.eventsProcessed)
        }

    @Test
    fun `reset limpia estado y repositorio`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            val snapshot =
                OfferSnapshot(
                    sessionId = "pipeline-123",
                    platform = RidePlatform.UBER,
                    capturedAtMillis = System.currentTimeMillis(),
                    source = SnapshotSource.REAL,
                    estimatedTotal = 125.0,
                    distanceKm = 8.5,
                    durationMin = 22.0,
                    detectionMillis = 50.0,
                )
            coordinator.onSnapshot(snapshot)
            repository.save(snapshot)
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

    @Test
    fun `coordinator consume snapshots del pipeline`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            val snapshot =
                OfferSnapshot(
                    sessionId = "pipeline-123",
                    platform = RidePlatform.UBER,
                    capturedAtMillis = System.currentTimeMillis(),
                    source = SnapshotSource.REAL,
                    estimatedTotal = 125.0,
                    distanceKm = 8.5,
                    durationMin = 22.0,
                    detectionMillis = 50.0,
                )
            coordinator.onSnapshot(snapshot)

            val state = coordinator.state.value
            assertNotNull(state.activeSession)
            assertEquals(RidePlatform.UBER, state.lastSnapshot?.platform)
            assertEquals("pipeline-123", state.lastSnapshot?.sessionId)
            assertEquals(1, state.activeSession?.capturedSnapshotCount)
            assertEquals(50.0, state.lastProcessingTimeMillis ?: 0.0, 0.0)
        }

    @Test
    fun `el tracking de snapshots acumula correctamente`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))

            val snapshot1 =
                OfferSnapshot(
                    sessionId = "pipeline-1",
                    platform = RidePlatform.UBER,
                    capturedAtMillis = System.currentTimeMillis(),
                    source = SnapshotSource.REAL,
                    estimatedTotal = 125.0,
                    distanceKm = 8.5,
                    durationMin = 22.0,
                    detectionMillis = 100.0,
                )
            val snapshot2 =
                OfferSnapshot(
                    sessionId = "pipeline-2",
                    platform = RidePlatform.UBER,
                    capturedAtMillis = System.currentTimeMillis(),
                    source = SnapshotSource.REAL,
                    estimatedTotal = 130.0,
                    distanceKm = 9.0,
                    durationMin = 25.0,
                    detectionMillis = 150.0,
                )
            coordinator.onSnapshot(snapshot1)

            pipeline.lastMetrics.value = ProcessingMetrics(totalMillis = 150.0)
            coordinator.onSnapshot(snapshot2)

            val state = coordinator.state.value
            assertEquals(2, state.activeSession?.capturedSnapshotCount)
            assertEquals(1, state.eventsProcessed)
            assertEquals(snapshot2, state.lastSnapshot)
        }

    @Test
    fun `el flow de windowEvents acumula eventos procesados`() =
        runBlocking {
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))
            coordinator.onWindowEvent(eventFor(RidePlatform.UBER.packageName))
            coordinator.onWindowEvent(eventFor(RidePlatform.DIDI.packageName))

            val state = coordinator.state.value
            assertEquals(3, state.eventsProcessed)
            assertEquals(3, state.recentEvents.size)
        }

    private fun eventFor(packageName: String): CaptureWindowEvent =
        CaptureWindowEvent(
            eventId = System.nanoTime(),
            packageName = packageName,
            eventType = com.sirc.capture.model.WindowEventType.WINDOW_STATE_CHANGED,
            timestampMillis = System.currentTimeMillis(),
            textCount = 5,
            fingerprint = "fp-$packageName",
        )

    private class FakeWindowObserver : WindowObserver {
        private val flow = MutableSharedFlow<CaptureWindowEvent>(extraBufferCapacity = 16)

        override val windowEvents: Flow<CaptureWindowEvent> = flow

        fun emitWindowEvent(event: CaptureWindowEvent) = flow.tryEmit(event)
    }

    private class FakePipeline : CapturePipeline {
        private val flow = MutableSharedFlow<OfferSnapshot>(extraBufferCapacity = 8)

        override val state = MutableStateFlow(OverlayState.WAITING)
        override val snapshots = flow
        override val lastMetrics = MutableStateFlow(ProcessingMetrics(totalMillis = 50.0))

        override suspend fun process(request: CaptureRequest): OfferSnapshot? {
            return null
        }

        fun emitSnapshot(snapshot: OfferSnapshot) = flow.tryEmit(snapshot)
    }
}
