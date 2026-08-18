package com.sirc.feature.overlay

import com.sirc.capture.log.SircLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que [OverlayController] no mienta sobre el estado de ejecución:
 * `start()` sin permiso de overlay no marca "en ejecución"; el arranque con
 * permiso delega en el launcher; y el servicio corrige el estado real vía
 * [OverlayController.onServiceRunning].
 */
class OverlayControllerTest {
    @Test
    fun `start sin permiso de overlay no arranca ni marca en ejecucion`() =
        runBlocking {
            val launcher = RecordingLauncher()
            val controller = controller(hasOverlayPermission = false, launcher = launcher)

            controller.start()

            assertEquals(0, launcher.starts)
            assertFalse(controller.isRunning.first())
        }

    @Test
    fun `start con permiso arranca y marca en ejecucion`() =
        runBlocking {
            val launcher = RecordingLauncher()
            val controller = controller(hasOverlayPermission = true, launcher = launcher)

            controller.start()

            assertEquals(1, launcher.starts)
            assertTrue(controller.isRunning.first())
        }

    @Test
    fun `stop detiene el servicio y apaga el estado`() =
        runBlocking {
            val launcher = RecordingLauncher()
            val controller = controller(hasOverlayPermission = true, launcher = launcher)

            controller.start()
            controller.stop()

            assertEquals(1, launcher.stops)
            assertFalse(controller.isRunning.first())
        }

    @Test
    fun `el servicio reporta su estado real al controlador`() =
        runBlocking {
            val controller = controller(hasOverlayPermission = true)

            controller.onServiceRunning(true)
            assertTrue(controller.isRunning.first())

            controller.onServiceRunning(false)
            assertFalse(controller.isRunning.first())
        }

    private fun controller(
        hasOverlayPermission: Boolean,
        launcher: OverlayServiceLauncher = RecordingLauncher(),
    ): OverlayController =
        OverlayController(
            launcher = launcher,
            permissions = FakePermissionManager(hasOverlayPermission = hasOverlayPermission),
            logger = FakeLogger(),
        )

    private class RecordingLauncher : OverlayServiceLauncher {
        var starts = 0
        var stops = 0

        override fun start() {
            starts++
        }

        override fun stop() {
            stops++
        }
    }

    private class FakePermissionManager(
        private val hasOverlayPermission: Boolean,
    ) : PermissionManager {
        override fun hasOverlayPermission(): Boolean = hasOverlayPermission

        override fun hasAccessibilityPermission(): Boolean = false

        override fun hasNotificationPermission(): Boolean = true

        override fun isIgnoringBatteryOptimizations(): Boolean = true

        override fun openOverlaySettings() = Unit

        override fun openAccessibilitySettings() = Unit

        override fun openNotificationSettings() = Unit

        override fun openBatteryOptimizationSettings() = Unit
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
