package com.sirc.feature.overlay

import kotlinx.coroutines.flow.StateFlow

/**
 * Fuente del estado que consume el OverlayService para renderizar su UI.
 *
 * En SPRINT 2 solo existe la implementación simulada; cuando se conecte el
 * análisis real, el `SircAccessibilityService`/`OfferEvaluator` podrá publicar
 * en una implementación que comparta esta misma interfaz.
 */
interface OverlayDataSource {
    val uiState: StateFlow<OverlayUiState>

    fun start()

    fun stop()
}
