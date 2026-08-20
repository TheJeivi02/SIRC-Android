package com.sirc.app

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sirc.feature.overlay.OverlayService
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

/**
 * Smoke instrumentado de [OverlayService] sobre el Application real (Hilt).
 *
 * Requiere SYSTEM_ALERT_WINDOW concedido (ya validado en DEVICE-01). No
 * automatiza interacción ni toca la implementación: verifica que el FGS
 * arranca bajo las condiciones requeridas, que WindowManager recibe la ventana
 * (Application Overlay) y que stop/removal no produce crash.
 */
@RunWith(AndroidJUnit4::class)
class OverlayServiceSmokeTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun servicioOverlay_seIniciaCreaVentanaYSeDetieneSinCrash() {
        val alertWindowGranted = Settings.canDrawOverlays(context)
        assertTrue(
            "SYSTEM_ALERT_WINDOW no concedido; requiere grant físico " +
                "(adb appops set com.sirc.app SYSTEM_ALERT_WINDOW allow)",
            alertWindowGranted,
        )

        try {
            OverlayService.start(context)
            assertTrue(
                "el servicio no arrancó dentro de ${TIMEOUT_MS}ms",
                waitUntil(TIMEOUT_MS) { serviceRunning() },
            )
            val windowOk = waitUntil(TIMEOUT_MS) { overlayWindowPresent() }
            assertTrue(
                "no se detectó la ventana Application Overlay dentro de ${TIMEOUT_MS}ms.\n" +
                    "DIAGNÓSTICO:\n${diagnose()}",
                windowOk,
            )
        } finally {
            OverlayService.stop(context)
        }

        assertTrue(
            "el servicio no se detuvo dentro de ${TIMEOUT_MS}ms",
            waitUntil(TIMEOUT_MS) { !serviceRunning() },
        )
    }

    private fun serviceRunning(): Boolean = shell("dumpsys activity services com.sirc.app").contains("OverlayService")

    private fun overlayWindowPresent(): Boolean {
        val windows = shell("dumpsys window windows")
        return windows.contains("ty=APPLICATION_OVERLAY") && windows.contains("u0 com.sirc.app}")
    }

    private fun diagnose(): String {
        val services = shell("dumpsys activity services com.sirc.app")
        val windows = shell("dumpsys window windows")
        val relevantWindows =
            windows
                .split("\n")
                .filter { it.contains("Window #") || it.contains("overlay", ignoreCase = true) }
                .joinToString("\n")
        return "--- services ---\n$services\n--- windows ---\n$relevantWindows"
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        descriptor.use { pfd ->
            return FileInputStream(pfd.fileDescriptor).bufferedReader().readText()
        }
    }

    private fun waitUntil(
        timeoutMs: Long,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(250)
        }
        return condition()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
