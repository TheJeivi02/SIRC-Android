package com.sirc.capture.android.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sirc.capture.log.SircLogger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test instrumentado de [MediaProjectionScreenCaptureProvider].
 *
 * No requiere otorgar el consentimiento del sistema: verifica el estado
 * inicial (sin proyección) y que sin proyección no se capturan frames.
 * La concesión real del permiso se prueba de forma manual (SPRINT_06_MANUAL_TEST).
 */
@RunWith(AndroidJUnit4::class)
class MediaProjectionScreenCaptureProviderTest {
    @Test
    fun sinPermisoConcedido_noProyectaNiCapturaFrames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val provider = MediaProjectionScreenCaptureProvider(context, NoOpSircLogger)

        assertFalse(provider.isProjecting.value)

        val frame = runBlocking { provider.captureFrame() }

        assertNull(frame)
    }

    private object NoOpSircLogger : SircLogger {
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
