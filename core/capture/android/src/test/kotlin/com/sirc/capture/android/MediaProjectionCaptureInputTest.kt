package com.sirc.capture.android

import android.content.Intent
import android.graphics.Bitmap
import com.sirc.capture.android.provider.ScreenCaptureProvider
import com.sirc.capture.log.SircLogger
import com.sirc.capture.model.CaptureRequest
import com.sirc.core.platform.CaptureInputType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaProjectionCaptureInputTest {
    private val logger = FakeLogger()
    private val provider = FakeScreenCaptureProvider()

    @Test
    fun `el origin del input es MEDIA_PROJECTION`() {
        val input = MediaProjectionCaptureInput(baseRequests(), provider, logger)
        assertEquals(CaptureInputType.MEDIA_PROJECTION, input.origin)
    }

    @Test
    fun `no proyecta pasa directo el request sin imagen`() =
        runBlocking {
            provider.isProjecting.value = false
            val input = MediaProjectionCaptureInput(baseRequests(), provider, logger)

            val result = input.requests().first()

            assertEquals(CaptureInputType.ACCESSIBILITY, result.origin)
            assertNull(result.imageData)
            assertEquals(listOf("oferta"), result.texts)
        }

    @Test
    fun `proyecta sin frame disponible degrada a los textos de accesibilidad`() =
        runBlocking {
            provider.isProjecting.value = true
            provider.frame = null
            val input = MediaProjectionCaptureInput(baseRequests(), provider, logger)

            val result = input.requests().first()

            assertEquals(CaptureInputType.ACCESSIBILITY, result.origin)
            assertNull(result.imageData)
        }

    @Test
    fun `proyecta con frame enriquece con imageData y origin MEDIA_PROJECTION`() {
        val request = request()
        val png = byteArrayOf(1, 2, 3)

        val enriched = enrichWithImage(request, png)

        assertEquals(CaptureInputType.MEDIA_PROJECTION, enriched.origin)
        assertArrayEquals(png, enriched.imageData)
        assertEquals(request.texts, enriched.texts)
    }

    private fun baseRequests() = flowOf(request())

    private fun request() =
        CaptureRequest(
            id = 1L,
            packageName = "com.ubercab",
            timestampMillis = System.currentTimeMillis(),
            texts = listOf("oferta"),
            origin = CaptureInputType.ACCESSIBILITY,
        )

    private class FakeScreenCaptureProvider : ScreenCaptureProvider {
        override val isProjecting: MutableStateFlow<Boolean> = MutableStateFlow(false)

        var frame: Bitmap? = null

        override fun onProjectionPermissionGranted(
            resultCode: Int,
            data: Intent?,
        ) = Unit

        override fun stopProjection() = Unit

        override suspend fun captureFrame(): Bitmap? = frame
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
