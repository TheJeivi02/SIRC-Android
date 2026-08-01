package com.sirc.capture.metrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rastrea los tiempos por etapa de las últimas ofertas procesadas (Debug).
 *
 * El pipeline registra cada oferta con [record]; el overlay completa la misma
 * oferta con [merge] (tiempos de evaluación/overlay). Expone las ofertas
 * recientes y el promedio de la ventana configurada.
 */
interface OfferPerformanceTracker {
    /** Últimas ofertas registradas, de la más antigua a la más reciente. */
    val lastOffers: StateFlow<List<OfferTiming>>

    /** Promedio por etapa de la ventana (por defecto, las últimas 20 ofertas). */
    val averages: StateFlow<OfferTiming>

    /** Registra una nueva oferta (pipeline). */
    fun record(timing: OfferTiming)

    /** Completa la oferta más reciente aún sin tiempos de evaluación/overlay. */
    fun merge(update: OfferTiming)

    fun clear()
}

/** Implementación en memoria, acotada a las últimas [MAX_OFFERS] ofertas. */
@Singleton
class InMemoryOfferPerformanceTracker @Inject constructor() : OfferPerformanceTracker {
    private val buffer = ArrayDeque<OfferTiming>()
    private val _lastOffers = MutableStateFlow<List<OfferTiming>>(emptyList())
    override val lastOffers: StateFlow<List<OfferTiming>> = _lastOffers.asStateFlow()

    private val _averages = MutableStateFlow(OfferTiming())
    override val averages: StateFlow<OfferTiming> = _averages.asStateFlow()

    @Synchronized
    override fun record(timing: OfferTiming) {
        buffer.addLast(timing)
        trim()
        publish()
    }

    @Synchronized
    override fun merge(update: OfferTiming) {
        val index = buffer.indices.lastOrNull { buffer[it].overlayMillis == null }
        if (index == null) {
            record(update)
            return
        }
        buffer[index] = buffer[index].merge(update)
        publish()
    }

    @Synchronized
    override fun clear() {
        buffer.clear()
        _lastOffers.value = emptyList()
        _averages.value = OfferTiming()
    }

    private fun trim() {
        while (buffer.size > MAX_OFFERS) buffer.removeFirst()
    }

    private fun publish() {
        _lastOffers.value = buffer.toList()
        _averages.value = averageOf(buffer, AVERAGE_WINDOW)
    }

    private fun averageOf(
        offers: List<OfferTiming>,
        window: Int,
    ): OfferTiming {
        val sample = offers.takeLast(window)

        fun average(select: (OfferTiming) -> Double?): Double? {
            val values = sample.mapNotNull(select)
            if (values.isEmpty()) return null
            return values.sum() / values.size
        }
        return OfferTiming(
            captureMillis = average { it.captureMillis },
            ocrMillis = average { it.ocrMillis },
            parseMillis = average { it.parseMillis },
            evaluationMillis = average { it.evaluationMillis },
            overlayMillis = average { it.overlayMillis },
            totalMillis = average { it.totalMillis },
        )
    }

    companion object {
        const val MAX_OFFERS = 100

        const val AVERAGE_WINDOW = 20
    }
}
