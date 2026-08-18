package com.sirc.feature.overlay

import com.sirc.capture.flag.FeatureFlag
import com.sirc.capture.flag.InMemoryFeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.metrics.InMemoryOfferPerformanceTracker
import com.sirc.capture.metrics.ProcessingMetrics
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.OverlayState
import com.sirc.capture.model.SnapshotSource
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.capture.validation.ValidationRecorder
import com.sirc.domain.engine.ConfidenceEngine
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.engine.ProfitEvaluationEngine
import com.sirc.domain.engine.RecommendationEngine
import com.sirc.domain.model.OfferHistoryEntry
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.repository.DriverConfigRepository
import com.sirc.domain.repository.OfferHistoryRepository
import com.sirc.domain.repository.OverlayConfigRepository
import com.sirc.domain.session.CaptureSessionManager
import com.sirc.domain.session.SessionStatus
import com.sirc.domain.usecase.EvaluateDetailedOfferUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineOverlayDataSourceTest {
    private val pipeline = FakeCapturePipeline()
    private val featureFlags = InMemoryFeatureFlags()
    private val performanceTracker = InMemoryOfferPerformanceTracker()
    private val evaluationRepository = InMemoryOfferEvaluationRepository()
    private val historyRepository = FakeOfferHistoryRepository()
    private val sessionManager = CaptureSessionManager()
    private val driverConfigRepository = FakeDriverConfigRepository()
    private val confidenceEngine = ConfidenceEngine()
    private val validationRecorder = ValidationRecorder()
    private val evaluateUseCase =
        EvaluateDetailedOfferUseCase(
            profitEvaluationEngine = ProfitEvaluationEngine(engine = ProfitEngine()),
            recommendationEngine = RecommendationEngine(),
            configRepository = driverConfigRepository,
        )

    private val dataSource =
        PipelineOverlayDataSource(
            pipeline = pipeline,
            evaluateUseCase = evaluateUseCase,
            configRepository = FakeOverlayConfigRepository(),
            featureFlags = featureFlags,
            logger = FakeLogger(),
            performanceTracker = performanceTracker,
            evaluationRepository = evaluationRepository,
            historyRepository = historyRepository,
            driverConfigRepository = driverConfigRepository,
            confidenceEngine = confidenceEngine,
            sessionManager = sessionManager,
            validationRecorder = validationRecorder,
        )

    @Test
    fun `estado inicial mantiene el overlay oculto`() =
        runBlocking {
            assertEquals(OverlayState.DISABLED, dataSource.uiState.value.status)
            assertFalse(dataSource.uiState.value.visible)
            assertNull(dataSource.uiState.value.evaluation)
            assertNull(dataSource.uiState.value.recommendation)
        }

    @Test
    fun `estado WAITING del pipeline marca el overlay visible`() =
        runBlocking {
            pipeline.state.value = OverlayState.WAITING

            waitFor { dataSource.uiState.value.status == OverlayState.WAITING }

            assertEquals(OverlayState.WAITING, dataSource.uiState.value.status)
            assertTrue(dataSource.uiState.value.visible)
        }

    @Test
    fun `start marca el overlay visible esperando oferta`() =
        runBlocking {
            dataSource.start()

            assertEquals(OverlayState.WAITING, dataSource.uiState.value.status)
            assertTrue(dataSource.uiState.value.visible)
            assertNull(dataSource.uiState.value.evaluation)
        }

    @Test
    fun `start con flag OVERLAY desactivada mantiene el overlay oculto`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.OVERLAY, false)

            dataSource.start()

            assertEquals(OverlayState.DISABLED, dataSource.uiState.value.status)
            assertFalse(dataSource.uiState.value.visible)
        }

    @Test
    fun `start y luego stop vuelven a ocultar el overlay`() =
        runBlocking {
            dataSource.start()
            dataSource.stop()

            assertEquals(OverlayState.DISABLED, dataSource.uiState.value.status)
            assertFalse(dataSource.uiState.value.visible)
        }

    @Test
    fun `estado ERROR del pipeline se refleja en el overlay`() =
        runBlocking {
            pipeline.state.value = OverlayState.ERROR

            waitFor { dataSource.uiState.value.status == OverlayState.ERROR }

            assertTrue(dataSource.uiState.value.visible)
        }

    @Test
    fun `snapshot evaluado muestra evaluación y recomendación ACCEPT`() =
        runBlocking {
            pipeline.snapshots.tryEmit(snapshot())

            waitFor { dataSource.uiState.value.evaluation != null }

            assertNotNull(dataSource.uiState.value.evaluation)
            assertNotNull(dataSource.uiState.value.recommendation)
            assertEquals(Recommendation.ACCEPT, dataSource.uiState.value.recommendation?.recommendation)
            assertTrue(dataSource.uiState.value.visible)
        }

    @Test
    fun `snapshot evaluado se registra en el historial temporal`() =
        runBlocking {
            pipeline.snapshots.tryEmit(snapshot())

            waitFor { dataSource.uiState.value.evaluation != null }

            val records = evaluationRepository.observe(limit = 10).first()
            val latest = records.single()
            assertEquals(RidePlatform.UBER, latest.platform)
            assertEquals(125.0, latest.price, 0.001)
            assertEquals(Recommendation.ACCEPT, latest.recommendation)
            assertTrue(latest.ocrText.isNotEmpty())
            assertNotNull(latest.parserResult)
        }

    @Test
    fun `snapshot evaluado expone tipo y confianza`() =
        runBlocking {
            pipeline.snapshots.tryEmit(snapshot(rawData = "type=UBER_REQUEST"))

            waitFor { dataSource.uiState.value.offerType != null }

            assertEquals("UBER_REQUEST", dataSource.uiState.value.offerType)
            assertNotNull(dataSource.uiState.value.confidence)
            assertNotNull(dataSource.uiState.value.ruleEvaluation)
            // ruleEvaluation se expone vacío (el motor de decisión es ProfitEngine)
            assertTrue(dataSource.uiState.value.ruleEvaluation!!.results.isEmpty())
        }

    @Test
    fun `stop oculta el overlay y vuelve a DISABLED`() =
        runBlocking {
            pipeline.state.value = OverlayState.PROCESSING
            waitFor { dataSource.uiState.value.status == OverlayState.PROCESSING }

            dataSource.stop()

            assertEquals(OverlayState.DISABLED, dataSource.uiState.value.status)
            assertFalse(dataSource.uiState.value.visible)
            assertNull(dataSource.uiState.value.evaluation)
            assertNull(dataSource.uiState.value.recommendation)
        }

    @Test
    fun `snapshot evaluado actualiza las estadisticas de sesion`() =
        runBlocking {
            pipeline.snapshots.tryEmit(snapshot())

            waitFor { sessionManager.stats.value.offersProcessed == 1 }

            assertEquals(SessionStatus.ACTIVE, sessionManager.stats.value.status)
            assertEquals(1, sessionManager.stats.value.offersProcessed)
            assertEquals(1, sessionManager.stats.value.offersAccepted)
        }

    @Test
    fun `estado ERROR del pipeline registra un error de sesion`() =
        runBlocking {
            pipeline.state.value = OverlayState.ERROR

            waitFor { sessionManager.stats.value.errors == 1 }

            assertEquals(1, sessionManager.stats.value.errors)
        }

    @Test
    fun `snapshot evaluado se persiste en el historial`() =
        runBlocking {
            pipeline.snapshots.tryEmit(snapshot(rawData = "type=UBER_REQUEST"))

            waitFor { historyRepository.entries.isNotEmpty() }

            val entry = historyRepository.entries.single()
            assertEquals(RidePlatform.UBER, entry.platform)
            assertEquals(Recommendation.ACCEPT, entry.recommendation)
            assertEquals("UBER_REQUEST", entry.offerType)
            assertNotNull(entry.confidencePercent)
            assertNotNull(entry.confidenceLevel)
            assertNotNull(entry.ruleSummary)
            assertNotNull(entry.processingMillis)
            assertNotNull(entry.evaluationMillis)
            assertNotNull(entry.rulesMillis)
        }

    @Test
    fun `flag OVERLAY desactivada degrada el estado a DISABLED`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.OVERLAY, false)
            pipeline.state.value = OverlayState.WAITING

            delay(50)

            assertEquals(OverlayState.DISABLED, dataSource.uiState.value.status)
            assertFalse(dataSource.uiState.value.visible)
        }

    private fun snapshot(rawData: String? = "data:test"): OfferSnapshot =
        OfferSnapshot(
            sessionId = "test-session",
            platform = RidePlatform.UBER,
            capturedAtMillis = System.currentTimeMillis(),
            source = SnapshotSource.REAL,
            estimatedTotal = 125.0,
            distanceKm = 8.5,
            durationMin = 22.0,
            rawData = rawData,
            texts = listOf("UBER", "$125.00", "8.5 km"),
        )

    private suspend fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timeout esperando condición del overlay" }
            delay(20)
        }
    }

    private class FakeCapturePipeline : CapturePipeline {
        override val state = MutableStateFlow(OverlayState.DISABLED)
        override val snapshots = MutableSharedFlow<OfferSnapshot>(extraBufferCapacity = 8)
        override val lastMetrics = MutableStateFlow(ProcessingMetrics())

        override suspend fun process(request: CaptureRequest): OfferSnapshot? = null
    }

    private class FakeDriverConfigRepository : DriverConfigRepository {
        override suspend fun getDriverConfig() = null

        override fun observeDriverConfig(): Flow<com.sirc.domain.model.DriverConfig?> = flowOf(null)

        override fun isConfigured(): Flow<Boolean> = flowOf(false)

        override suspend fun save(driverConfig: com.sirc.domain.model.DriverConfig) = Unit
    }

    private class FakeOverlayConfigRepository : OverlayConfigRepository {
        private val config = MutableStateFlow(OverlayConfig())

        override suspend fun getOverlayConfig(): OverlayConfig = config.value

        override suspend fun save(overlayConfig: OverlayConfig) {
            config.value = overlayConfig
        }

        override fun observeOverlayConfig(): Flow<OverlayConfig> = config.asStateFlow()
    }

    private class FakeOfferHistoryRepository : OfferHistoryRepository {
        val entries = mutableListOf<OfferHistoryEntry>()

        override suspend fun add(entry: OfferHistoryEntry) {
            entries.add(entry)
        }

        override suspend fun clear() {
            entries.clear()
        }

        override suspend fun trimToLimit(limit: Int) {
            while (entries.size > limit) entries.removeAt(0)
        }

        override fun observeEntries(limit: Int): Flow<List<OfferHistoryEntry>> = flowOf(entries.take(limit).toList())
    }

    private class FakeLogger : SircLogger {
        override fun debug(
            tag: String,
            message: String,
        ) = Unit

        override fun info(
            tag: String,
            message: String,
        ) = Unit

        override fun warn(
            tag: String,
            message: String,
        ) = Unit

        override fun error(
            tag: String,
            message: String,
        ) = Unit
    }
}
