package com.sirc.capture.repository

import com.sirc.capture.model.OfferSnapshot
import com.sirc.capture.model.SnapshotSource
import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryCaptureRepositoryTest {
    private val repository = InMemoryCaptureRepository()

    @Test
    fun `guarda y recupera el último snapshot`() {
        repository.save(snapshot(1))

        assertEquals(1L, repository.latestSnapshot()?.capturedAtMillis)
        assertEquals(1, repository.snapshots().size)
    }

    @Test
    fun `limpia todos los snapshots`() {
        repository.save(snapshot(1))
        repository.clear()

        assertNull(repository.latestSnapshot())
        assertEquals(0, repository.snapshots().size)
    }

    @Test
    fun `mantiene solo los snapshots más recientes`() {
        repeat(60) { index -> repository.save(snapshot(index.toLong())) }

        assertEquals(50, repository.snapshots().size)
        assertEquals(59L, repository.latestSnapshot()?.capturedAtMillis)
    }

    private fun snapshot(capturedAtMillis: Long): OfferSnapshot =
        OfferSnapshot(
            sessionId = "session-1",
            platform = RidePlatform.UBER,
            capturedAtMillis = capturedAtMillis,
            source = SnapshotSource.FAKE,
            estimatedTotal = 125.0,
            distanceKm = 8.5,
            durationMin = 22.0,
        )
}
