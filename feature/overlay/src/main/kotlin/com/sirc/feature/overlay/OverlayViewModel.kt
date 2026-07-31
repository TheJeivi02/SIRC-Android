package com.sirc.feature.overlay

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Estado del overlay para las pantallas (vista previa en Diagnóstico).
 *
 * Comparte la misma fuente que el [OverlayService] ([OverlayDataSource]):
 * una única fuente de verdad para el estado del overlay.
 */
@HiltViewModel
class OverlayViewModel @Inject constructor(
    private val dataSource: OverlayDataSource,
) : ViewModel() {
    val uiState: StateFlow<OverlayUiState> = dataSource.uiState

    fun start() = dataSource.start()

    fun stop() = dataSource.stop()
}
