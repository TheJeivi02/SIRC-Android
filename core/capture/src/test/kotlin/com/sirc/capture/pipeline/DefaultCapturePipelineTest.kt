package com.sirc.capture.pipeline

import com.sirc.capture.cache.InMemoryCaptureFrameCache
import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.InMemoryFeatureFlags
import com.sirc.capture.log.TestLogger
import com.sirc.capture.metrics.CaptureMetrics
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.model.OfferCaptureSession
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.model.ScreenFrame
import com.sirc.capture.model.SnapshotSource
import com.sirc.capture.ocr.OcrEngine
import com.sirc.capture.parser.OfferParser
import com.sirc.capture.repository.InMemoryCaptureRepository
import com.sirc.capture.screen.ScreenCapture
import com.sirc.domain.model.RidePlatform
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class DefaultCapturePipelineTest {
    private val screenCapture = FakeScreenCapture()
    private val ocrEngine = FakeOcrEngine()
    private val parser = FakeOfferParser()
    private val repository = InMemoryCaptureRepository()
    private val featureFlags = InMemoryFeatureFlags()
    private val cache = InMemoryCaptureFrameCache()
    private val metrics = RecordingCaptureMetrics()
    private val logger = TestLogger()

    private val pipeline =
        DefaultCapturePipeline(
            screenCapture = screenCapture,
            ocrEngine = ocrEngine,
            parser = parser,
            repository = repository,
            featureFlags = featureFlags,
            cache = cache,
            metrics = metrics,
            logger = logger,
        )

    @Test
    fun `solicitud con texto produce snapshot y lo guarda`() =
        runBlocking {
            val snapshot = pipeline.process(requestFor(texts = listOf("UBER", "$125.00")))

            assertNotNull(snapshot)
            assertEquals(RidePlatform.UBER, snapshot?.platform)
            assertEquals(SnapshotSource.REAL, snapshot?.source)
            assertEquals(1, repository.snapshots().size)
            assertEquals(OverlayState.WAITING, pipeline.state.value)
        }

    @Test
    fun `solicitud con imagen ejecuta OCR y guarda snapshot`() =
        runBlocking {
            val image = loadTestImage("test-images/offer_uber_1.png")
            ocrEngine.recognized = listOf("UBER", "$125.00", "8.5 km")

            val snapshot = pipeline.process(requestFor(imageData = image))

            assertNotNull(snapshot)
            assertArrayEquals(image, ocrEngine.lastImage)
            assertEquals(1, repository.snapshots().size)
        }

    @Test
    fun `flag OCR desactivada no ejecuta el motor`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.OCR, false)
            ocrEngine.recognized = listOf("UBER", "$125.00")

            val snapshot = pipeline.process(requestFor(imageData = loadTestImage("test-images/offer_uber_1.png")))

            assertNull(snapshot)
            assertNull(ocrEngine.lastImage)
            assertEquals(0, repository.snapshots().size)
        }

    @Test
    fun `flag CAPTURE desactivada ignora la solicitud`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.CAPTURE, false)

            val snapshot = pipeline.process(requestFor(texts = listOf("UBER", "$125.00")))

            assertNull(snapshot)
            assertEquals(0, repository.snapshots().size)
            assertEquals(OverlayState.DISABLED, pipeline.state.value)
        }

    @Test
    fun `sin texto ni imagen devuelve null sin guardar`() =
        runBlocking {
            val snapshot = pipeline.process(requestFor())

            assertNull(snapshot)
            assertEquals(0, repository.snapshots().size)
            assertEquals(OverlayState.WAITING, pipeline.state.value)
        }

    @Test
    fun `fallo en la captura de pantalla pasa a ERROR`() =
        runBlocking {
            screenCapture.fail = true

            val snapshot = pipeline.process(requestFor(texts = listOf("UBER", "$125.00")))

            assertNull(snapshot)
            assertEquals(0, repository.snapshots().size)
            assertEquals(OverlayState.ERROR, pipeline.state.value)
        }

    @Test
    fun `fallo del OCR pasa a ERROR`() =
        runBlocking {
            ocrEngine.throwError = true

            val snapshot = pipeline.process(requestFor(imageData = loadTestImage("test-images/offer_uber_1.png")))

            assertNull(snapshot)
            assertEquals(OverlayState.ERROR, pipeline.state.value)
        }

    @Test
    fun `parser sin resultado no guarda snapshot`() =
        runBlocking {
            parser.result = null

            val snapshot = pipeline.process(requestFor(texts = listOf("UBER", "$125.00")))

            assertNull(snapshot)
            assertEquals(0, repository.snapshots().size)
            assertEquals(OverlayState.WAITING, pipeline.state.value)
        }

    @Test
    fun `imagen de prueba carga y recorre el pipeline`() =
        runBlocking {
            val image = loadTestImage("test-images/offer_uber_1.png")
            assertTrue(image.isNotEmpty())
            ocrEngine.recognized = listOf("UBER", "$125.00", "8.5 km", "22 min")

            val snapshot = pipeline.process(requestFor(imageData = image))

            assertNotNull(snapshot)
            assertTrue(ocrEngine.calls > 0)
            assertEquals(1, repository.snapshots().size)
        }

    @Test
    fun `captura idéntica se omite por caché y no repite OCR`() =
        runBlocking {
            val image = loadTestImage("test-images/offer_uber_1.png")
            ocrEngine.recognized = listOf("UBER", "$125.00", "8.5 km")

            pipeline.process(requestFor(imageData = image))
            assertEquals(1, ocrEngine.calls)
            assertEquals(1, repository.snapshots().size)

            pipeline.process(requestFor(imageData = image))

            assertEquals(1, ocrEngine.calls)
            assertEquals(1, repository.snapshots().size)
        }

    @Test
    fun `snapshot se emite al flow del pipeline`() =
        runBlocking {
            val emitted = mutableListOf<OfferSnapshot>()
            val collectJob =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    pipeline.snapshots.collect { emitted += it }
                }

            pipeline.process(requestFor(texts = listOf("UBER", "$125.00")))
            yield()
            collectJob.cancel()

            assertEquals(1, emitted.size)
            assertEquals(RidePlatform.UBER, emitted.first().platform)
        }

    @Test
    fun `métricas de cada etapa se registran y quedan disponibles`() =
        runBlocking {
            ocrEngine.recognized = listOf("UBER", "$125.00")

            pipeline.process(requestFor(imageData = loadTestImage("test-images/offer_uber_1.png")))

            assertTrue(metrics.captureCalls > 0)
            assertTrue(metrics.ocrCalls > 0)
            assertTrue(metrics.parseCalls > 0)
            val last = pipeline.lastMetrics.value
            assertNotNull(last.captureMillis)
            assertNotNull(last.ocrMillis)
            assertNotNull(last.parseMillis)
            assertNotNull(last.totalMillis)
        }

    private fun requestFor(
        texts: List<String> = emptyList(),
        imageData: ByteArray? = null,
    ): CaptureRequest =
        CaptureRequest(
            id = System.nanoTime(),
            packageName = RidePlatform.UBER.packageName,
            timestampMillis = System.currentTimeMillis(),
            texts = texts,
            imageData = imageData,
        )

    private fun loadTestImage(resource: String): ByteArray {
        val stream = javaClass.classLoader?.getResourceAsStream(resource) ?: error("Recurso no encontrado: $resource")
        return stream.use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                output.toByteArray()
            }
        }
    }

    private class FakeScreenCapture : ScreenCapture {
        var fail = false

        override suspend fun capture(request: CaptureRequest): ScreenFrame? {
            if (fail) return null
            return ScreenFrame(
                requestId = request.id,
                packageName = request.packageName,
                timestampMillis = request.timestampMillis,
                texts = request.texts,
                imageData = request.imageData,
            )
        }
    }

    private class FakeOcrEngine : OcrEngine {
        var recognized: List<String> = emptyList()
        var throwError = false
        var calls = 0
        var lastImage: ByteArray? = null

        override suspend fun recognize(imageData: ByteArray): List<String> {
            calls++
            lastImage = imageData
            if (throwError) error("OCR boom")
            return recognized
        }
    }

    private class FakeOfferParser : OfferParser {
        var result: OfferSnapshot? = fakeSnapshot()

        override fun parse(
            event: CaptureWindowEvent,
            session: OfferCaptureSession,
        ): OfferSnapshot? = result

        private fun fakeSnapshot(): OfferSnapshot =
            OfferSnapshot(
                sessionId = "test-session",
                platform = RidePlatform.UBER,
                capturedAtMillis = System.currentTimeMillis(),
                source = SnapshotSource.REAL,
                estimatedTotal = 125.0,
                distanceKm = 8.5,
                durationMin = 22.0,
            )
    }

    private class RecordingCaptureMetrics : CaptureMetrics {
        var captureCalls = 0
        var ocrCalls = 0
        var parseCalls = 0
        var totalCalls = 0

        override fun onCapture(millis: Double) {
            captureCalls++
        }

        override fun onOcr(millis: Double) {
            ocrCalls++
        }

        override fun onParse(millis: Double) {
            parseCalls++
        }

        override fun onTotal(millis: Double) {
            totalCalls++
        }
    }
}
