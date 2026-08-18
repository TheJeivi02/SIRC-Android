package com.sirc.feature.overlay

import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.CaptureWindowEvent
import com.sirc.capture.scheduler.DebounceCaptureScheduler
import com.sirc.core.platform.PlatformDescriptorRegistry
import com.sirc.core.platform.PlatformDescriptors
import com.sirc.core.platform.PlatformDetectionEngine
import com.sirc.domain.model.DriverConfig
import com.sirc.domain.model.RidePlatform
import com.sirc.domain.repository.DriverConfigRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityCaptureInputTest {
    private val detectionEngine =
        PlatformDetectionEngine(PlatformDescriptorRegistry(PlatformDescriptors.all()))

    @Test
    fun `plataforma no activa es rechazada y la activa aceptada`() {
        val input = createInput(initialPlatforms = null)
        input.setActivePlatforms(setOf(RidePlatform.UBER))

        assertTrue(input.isPlatformActive(RidePlatform.UBER))
        assertFalse(input.isPlatformActive(RidePlatform.INDRIVE))
        assertFalse(input.isPlatformActive(RidePlatform.DIDI))
    }

    @Test
    fun `conjunto de plataformas vacio acepta todas (comportamiento legado)`() {
        val input = createInput(initialPlatforms = null)
        input.setActivePlatforms(emptySet())

        assertTrue(input.isPlatformActive(RidePlatform.UBER))
        assertTrue(input.isPlatformActive(RidePlatform.INDRIVE))
        assertTrue(input.isPlatformActive(RidePlatform.CABIFY))
    }

    @Test
    fun `las plataformas configuradas del conductor se aplican desde el repositorio`() =
        runBlocking {
            val repo =
                FakeDriverConfigRepository(
                    config = DriverConfig.default().copy(platforms = setOf(RidePlatform.INDRIVE)),
                )
            val input =
                AccessibilityCaptureInput(
                    scheduler = DebounceCaptureScheduler(FakeLogger()),
                    windowEventPublisher = FakeWindowEventPublisher(),
                    detectionEngine = detectionEngine,
                    driverConfigRepository = repo,
                    logger = FakeLogger(),
                )

            waitUntil {
                input.currentActivePlatforms == setOf(RidePlatform.INDRIVE)
            }

            assertEquals(setOf(RidePlatform.INDRIVE), input.currentActivePlatforms)
            assertTrue(input.isPlatformActive(RidePlatform.INDRIVE))
            assertFalse(input.isPlatformActive(RidePlatform.UBER))
        }

    private fun createInput(initialPlatforms: Set<RidePlatform>?): AccessibilityCaptureInput =
        AccessibilityCaptureInput(
            scheduler = DebounceCaptureScheduler(FakeLogger()),
            windowEventPublisher = FakeWindowEventPublisher(),
            detectionEngine = detectionEngine,
            driverConfigRepository = FakeDriverConfigRepository(initialPlatforms = initialPlatforms),
            logger = FakeLogger(),
        )

    private suspend fun waitUntil(condition: () -> Boolean) {
        repeat(100) {
            if (condition()) return
            delay(10)
        }
        assertTrue("condición no cumplida", condition())
    }

    private class FakeDriverConfigRepository(
        val config: DriverConfig? = null,
        initialPlatforms: Set<RidePlatform>? = null,
    ) : DriverConfigRepository {
        private val flow: MutableStateFlow<DriverConfig?> =
            MutableStateFlow(
                initialPlatforms?.let { config?.copy(platforms = it) ?: DriverConfig.default().copy(platforms = it) }
                    ?: config,
            )

        override suspend fun getDriverConfig(): DriverConfig? = flow.value

        override fun observeDriverConfig(): Flow<DriverConfig?> = flow

        override fun isConfigured(): Flow<Boolean> = MutableStateFlow(flow.value != null)

        override suspend fun save(driverConfig: DriverConfig) {
            flow.value = driverConfig
        }
    }

    private class FakeWindowEventPublisher : WindowEventPublisher {
        override fun onWindowEvent(event: CaptureWindowEvent) = Unit
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
