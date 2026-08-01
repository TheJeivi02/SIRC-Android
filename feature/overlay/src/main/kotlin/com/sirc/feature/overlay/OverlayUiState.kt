package com.sirc.feature.overlay

import com.sirc.capture.model.OverlayState
import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.ProfitEvaluation

/**
 * Estado observable que consume el OverlayService para renderizar su UI.
 *
 * [status] refleja el estado real del [com.sirc.capture.pipeline.CapturePipeline]
 * (WAITING/CAPTURING/PROCESSING/ERROR); [evaluation] es el resultado del
 * análisis cuando hay una oferta evaluada.
 */
data class OverlayUiState(
    val evaluation: ProfitEvaluation? = null,
    val config: OverlayConfig = OverlayConfig(),
    val status: OverlayState = OverlayState.DISABLED,
    val visible: Boolean = false,
)
