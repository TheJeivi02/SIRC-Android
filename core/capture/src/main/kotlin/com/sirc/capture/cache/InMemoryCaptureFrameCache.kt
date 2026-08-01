package com.sirc.capture.cache

import com.sirc.capture.model.ScreenFrame
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caché en memoria acotada: retiene los hashes de los últimos [MAX_ENTRIES]
 * frames con imagen y evita reprocesar capturas idénticas.
 */
@Singleton
class InMemoryCaptureFrameCache @Inject constructor() : CaptureFrameCache {
    private val seen =
        object : LinkedHashMap<String, Boolean>(MAX_ENTRIES, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean =
                size > MAX_ENTRIES
        }

    @Synchronized
    override fun isNew(frame: ScreenFrame): Boolean {
        val key = frameKey(frame) ?: return true
        return !seen.containsKey(key)
    }

    @Synchronized
    override fun markProcessed(frame: ScreenFrame) {
        val key = frameKey(frame) ?: return
        seen[key] = true
    }

    @Synchronized
    override fun clear() = seen.clear()

    /** Clave del frame: hash estable del contenido de imagen, si lo hay. */
    private fun frameKey(frame: ScreenFrame): String? =
        frame.imageData?.let { bytes -> "img-${bytes.contentHashCode()}" }

    companion object {
        private const val MAX_ENTRIES = 32
        private const val LOAD_FACTOR = 0.75f
    }
}
