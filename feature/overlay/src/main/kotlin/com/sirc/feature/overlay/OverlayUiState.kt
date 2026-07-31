package com.sirc.feature.overlay

import com.sirc.domain.model.OverlayConfig
import com.sirc.domain.model.ProfitEvaluation

/** Estado observable que consume el OverlayService para renderizar su UI. */
data class OverlayUiState(
    val evaluation: ProfitEvaluation? = null,
    val config: OverlayConfig = OverlayConfig(),
    val visible: Boolean = false,
)
