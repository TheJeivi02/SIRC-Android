package com.sirc.capture.scheduler

import com.sirc.capture.model.CaptureRequest
import com.sirc.domain.model.RidePlatform
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebounceCaptureSchedulerTest {
    private val scheduler = DebounceCaptureScheduler()

    @Test
    fun `emite solo el último request tras el debounce`() =
        runTest {
            val emitted = mutableListOf<Long>()
            backgroundScope.launch {
                scheduler.debouncedRequests(debounceMillis = 100L).collect { emitted += it.id }
            }
            runCurrent()

            scheduler.schedule(request(1L))
            scheduler.schedule(request(2L))
            scheduler.schedule(request(3L))
            advanceTimeBy(200L)

            assertEquals(listOf(3L), emitted)
        }

    @Test
    fun `no emite nada si llega un solo request y no pasa el debounce`() =
        runTest {
            val emitted = mutableListOf<Long>()
            backgroundScope.launch {
                scheduler.debouncedRequests(debounceMillis = 100L).collect { emitted += it.id }
            }
            runCurrent()

            scheduler.schedule(request(1L))
            advanceTimeBy(50L)

            assertEquals(emptyList<Long>(), emitted)
        }

    @Test
    fun `reinicia el debounce ante un nuevo request`() =
        runTest {
            val emitted = mutableListOf<Long>()
            backgroundScope.launch {
                scheduler.debouncedRequests(debounceMillis = 100L).collect { emitted += it.id }
            }
            runCurrent()

            scheduler.schedule(request(1L))
            advanceTimeBy(80L)
            scheduler.schedule(request(2L))
            advanceTimeBy(80L)
            assertEquals(emptyList<Long>(), emitted)

            advanceTimeBy(120L)
            assertEquals(listOf(2L), emitted)
        }

    private fun request(id: Long): CaptureRequest =
        CaptureRequest(
            id = id,
            packageName = RidePlatform.UBER.packageName,
            timestampMillis = id,
            texts = emptyList(),
        )
}
