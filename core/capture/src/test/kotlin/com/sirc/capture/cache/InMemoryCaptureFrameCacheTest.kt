package com.sirc.capture.cache

import com.sirc.capture.model.CaptureRequest
import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCaptureFrameCacheTest {
    private val cache = InMemoryCaptureFrameCache()

    @Test
    fun `request sin imagen nunca se considera repetido`() {
        val request = request(packageName = "com.test", texts = emptyList())
        assertTrue(cache.isNew(request))
    }

    @Test
    fun `request ya procesado no es nuevo`() {
        val image = byteArrayOf(1, 2, 3, 4)
        val request = request(packageName = "com.test", imageData = image)
        cache.markProcessed(request)

        assertFalse(cache.isNew(request))
    }

    @Test
    fun `imagen idéntica en diferentes requests se considera repetida`() {
        val image = byteArrayOf(9, 8, 7, 6)
        val request1 = request(packageName = "com.test1", id = 1L, imageData = image)
        val request2 = request(packageName = "com.test2", id = 2L, imageData = image)
        cache.markProcessed(request1)

        assertFalse(cache.isNew(request2))
    }

    @Test
    fun `imagen distinta en request siempre es nueva`() {
        val image = byteArrayOf(1, 2, 3)
        val request = request(packageName = "com.test", imageData = image)
        cache.markProcessed(request)

        assertTrue(cache.isNew(request(packageName = "com.test", imageData = byteArrayOf(4, 5, 6))))
    }

    @Test
    fun `clear vacía la caché de requests`() {
        val image = byteArrayOf(1, 2, 3)
        val request = request(packageName = "com.test", imageData = image)
        cache.markProcessed(request)
        cache.clear()

        assertTrue(cache.isNew(request))
    }

    private fun request(
        id: Long = 1L,
        packageName: String = RidePlatform.UBER.packageName,
        texts: List<String> = emptyList(),
        imageData: ByteArray? = null,
    ): CaptureRequest =
        CaptureRequest(
            id = id,
            packageName = packageName,
            timestampMillis = id,
            texts = texts,
            imageData = imageData,
        )
}
