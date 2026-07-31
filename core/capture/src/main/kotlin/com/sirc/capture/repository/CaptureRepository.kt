package com.sirc.capture.repository

import com.sirc.capture.model.OfferSnapshot

/**
 * Guarda temporalmente los [OfferSnapshot] capturados.
 *
 * Interfaz preparada para futuras implementaciones (por ejemplo persistencia
 * en Room); hoy solo existe la variante en memoria.
 */
interface CaptureRepository {
    fun save(snapshot: OfferSnapshot)

    fun latestSnapshot(): OfferSnapshot?

    fun snapshots(): List<OfferSnapshot>

    fun clear()
}
