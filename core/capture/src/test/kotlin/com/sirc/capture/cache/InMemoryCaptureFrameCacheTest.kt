package com.sirc.capture.cache

import com.sirc.capture.model.ScreenFrame
import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCaptureFrameCacheTest {
    private val cache = InMemoryCaptureFrameCache()

    @Test
    fun `frame sin imagen nunca se considera repetido`() {
        assertTrue(cache.isNew(frame(imageData = null)))
    }

    @Test
    fun `frame ya procesado no es nuevo`() {
        val image = byteArrayOf(1, 2, 3, 4)
        cache.markProcessed(frame(imageData = image))

        assertFalse(cache.isNew(frame(imageData = image)))
    }

    @Test
    fun `imagen idéntica con distinto request se considera repetida`() {
        val image = byteArrayOf(9, 8, 7, 6)
        cache.markProcessed(frame(id = 1L, imageData = image))

        assertFalse(cache.isNew(frame(id = 2L, imageData = image)))
    }

    @Test
    fun `imagen distinta siempre es nueva`() {
        cache.markProcessed(frame(imageData = byteArrayOf(1, 2, 3)))

        assertTrue(cache.isNew(frame(imageData = byteArrayOf(4, 5, 6))))
    }

    @Test
    fun `clear vacía la caché`() {
        val image = byteArrayOf(1, 2, 3)
        cache.markProcessed(frame(imageData = image))
        cache.clear()

        assertTrue(cache.isNew(frame(imageData = image)))
    }

    private fun frame(
        id: Long = 1L,
        imageData: ByteArray?,
    ): ScreenFrame =
        ScreenFrame(
            requestId = id,
            packageName = RidePlatform.UBER.packageName,
            timestampMillis = id,
            texts = emptyList(),
            imageData = imageData,
        )
}
