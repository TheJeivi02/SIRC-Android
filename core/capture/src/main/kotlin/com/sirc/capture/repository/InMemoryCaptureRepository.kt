package com.sirc.capture.repository

import com.sirc.capture.model.OfferSnapshot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación en memoria de [CaptureRepository]: retiene los snapshots más
 * recientes durante la vida del proceso.
 */
@Singleton
class InMemoryCaptureRepository @Inject constructor() : CaptureRepository {
    private val buffer = mutableListOf<OfferSnapshot>()

    @Synchronized
    override fun save(snapshot: OfferSnapshot) {
        buffer += snapshot
        while (buffer.size > MAX_SNAPSHOTS) buffer.removeAt(0)
    }

    @Synchronized
    override fun latestSnapshot(): OfferSnapshot? = buffer.lastOrNull()

    @Synchronized
    override fun snapshots(): List<OfferSnapshot> = buffer.toList()

    @Synchronized
    override fun clear() = buffer.clear()

    companion object {
        private const val MAX_SNAPSHOTS = 50
    }
}
