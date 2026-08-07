package com.sirc.capture.input

import com.sirc.capture.model.CaptureRequest
import com.sirc.core.platform.CaptureInputType
import kotlinx.coroutines.flow.Flow

/**
 * Entrada de captura genérica.
 *
 * Cada origen de captura (Accessibility, MediaProjection, galería, tests)
 * expone un Flow de CaptureRequest a través de esta interfaz.
 *
 * No contiene lógica de OCR ni de parsing; solo genera solicitudes.
 */
interface CaptureInput {
    /** Tipo de origen de esta entrada. */
    val origin: CaptureInputType

    /** Flujo de solicitudes de captura generadas por esta entrada. */
    fun requests(): Flow<CaptureRequest>
}
