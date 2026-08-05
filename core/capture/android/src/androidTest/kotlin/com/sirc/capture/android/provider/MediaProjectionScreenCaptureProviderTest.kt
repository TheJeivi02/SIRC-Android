package com.sirc.capture.android.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sirc.capture.log.SircLogger
import com.sirc.capture.validation.ValidationRecorder
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
        val provider = createProvider()

        assertFalse(provider.isProjecting.value)

        val frame = runBlocking { provider.captureFrame() }

        assertNull(frame)
    }

    @Test
    fun stopProjection_sinProyeccionActiva_esIdempotente() {
        val provider = createProvider()

        provider.stopProjection()
        provider.stopProjection()

        assertFalse(provider.isProjecting.value)
        assertNull(runBlocking { provider.captureFrame() })
    }

    @Test
    fun onServiceDestroyed_liberaRecursosYDejaDeProyectar() {
        val provider = createProvider()

        provider.onServiceDestroyed()

        assertFalse(provider.isProjecting.value)
        assertNull(runBlocking { provider.captureFrame() })
    }

    @Test
    fun onServiceDestroyed_esIdempotente_noLanzaAlRepetirse() {
        val provider = createProvider()

        provider.onServiceDestroyed()
        provider.onServiceDestroyed()
        provider.onServiceDestroyed()

        assertFalse(provider.isProjecting.value)
    }

    @Test
    fun stopProjectionYOnServiceDestroyed_combinadosNoLanzan() {
        val provider = createProvider()

        provider.stopProjection()
        provider.onServiceDestroyed()
        provider.stopProjection()

        assertFalse(provider.isProjecting.value)
    }

    private fun createProvider(): MediaProjectionScreenCaptureProvider {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return MediaProjectionScreenCaptureProvider(context, NoOpSircLogger, ValidationRecorder())
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
