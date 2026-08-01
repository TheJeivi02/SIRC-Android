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
import com.sirc.domain.engine.ProfitEngine
import com.sirc.domain.engine.ProfitEvaluationEngine
import com.sirc.domain.engine.RecommendationEngine
import com.sirc.domain.model.DecisionThresholds
import com.sirc.domain.model.DriverCosts
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.Recommendation
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.repository.DriverConfigRepository
import com.sirc.domain.repository.OverlayConfigRepository
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
    private val historyRepository = InMemoryOfferEvaluationRepository()
    private val evaluateUseCase =
        EvaluateDetailedOfferUseCase(
            profitEvaluationEngine = ProfitEvaluationEngine(engine = ProfitEngine()),
            recommendationEngine = RecommendationEngine(),
            configRepository = FakeDriverConfigRepository(),
        )

    private val dataSource =
        PipelineOverlayDataSource(
            pipeline = pipeline,
            evaluateUseCase = evaluateUseCase,
            configRepository = FakeOverlayConfigRepository(),
            featureFlags = featureFlags,
            logger = FakeLogger(),
            performanceTracker = performanceTracker,
            historyRepository = historyRepository,
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

            val records = historyRepository.observe(limit = 10).first()
            val latest = records.single()
            assertEquals(RidePlatform.UBER, latest.platform)
            assertEquals(125.0, latest.price, 0.001)
            assertEquals(Recommendation.ACCEPT, latest.recommendation)
            assertTrue(latest.ocrText.isNotEmpty())
            assertNotNull(latest.parserResult)
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
    fun `flag OVERLAY desactivada degrada el estado a DISABLED`() =
        runBlocking {
            featureFlags.setEnabled(FeatureFlag.OVERLAY, false)
            pipeline.state.value = OverlayState.CAPTURING

            delay(50)

            assertEquals(OverlayState.DISABLED, dataSource.uiState.value.status)
            assertFalse(dataSource.uiState.value.visible)
        }

    private fun snapshot(): OfferSnapshot =
        OfferSnapshot(
            sessionId = "test-session",
            platform = RidePlatform.UBER,
            capturedAtMillis = System.currentTimeMillis(),
            source = SnapshotSource.REAL,
            estimatedTotal = 125.0,
            distanceKm = 8.5,
            durationMin = 22.0,
            rawData = "data:test",
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
        private val costs = DriverCosts.default()
        private val thresholds = DecisionThresholds.default()

        override suspend fun getDriverConfig() = null

        override fun observeDriverConfig(): Flow<com.sirc.domain.model.DriverConfig?> = flowOf(null)

        override fun isConfigured(): Flow<Boolean> = flowOf(false)

        override suspend fun save(driverConfig: com.sirc.domain.model.DriverConfig) = Unit

        override suspend fun getDriverCosts(): DriverCosts = costs

        override suspend fun getDecisionThresholds(): DecisionThresholds = thresholds

        override suspend fun save(driverCosts: DriverCosts) = Unit

        override suspend fun save(decisionThresholds: DecisionThresholds) = Unit

        override fun observeDriverCosts(): Flow<DriverCosts> = flowOf(costs)

        override fun observeDecisionThresholds(): Flow<DecisionThresholds> = flowOf(thresholds)
    }

    private class FakeOverlayConfigRepository : OverlayConfigRepository {
        private val config = MutableStateFlow(OverlayConfig())

        override suspend fun getOverlayConfig(): OverlayConfig = config.value

        override suspend fun save(overlayConfig: OverlayConfig) {
            config.value = overlayConfig
        }

        override fun observeOverlayConfig(): Flow<OverlayConfig> = config.asStateFlow()
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
