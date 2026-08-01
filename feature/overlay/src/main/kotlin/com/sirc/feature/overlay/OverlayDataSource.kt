package com.sirc.feature.overlay

import kotlinx.coroutines.flow.StateFlow

/**
 * Fuente del estado que consume el OverlayService para renderizar su UI.
 *
 * La implementación real es [PipelineOverlayDataSource], que expone el estado
 * del pipeline de captura + evaluación.
 */
interface OverlayDataSource {
    val uiState: StateFlow<OverlayUiState>

    fun start()

    fun stop()
}
