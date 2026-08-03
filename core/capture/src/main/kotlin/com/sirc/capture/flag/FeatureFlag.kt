package com.sirc.capture.flag

/**
 * Flags de características configurables en tiempo de ejecución desde el
 * panel de depuración (Modo Beta).
 *
 * WP-E1-02: se eliminó `RULES` (ya no hay alternancia entre motores de
 * decisión; `ProfitEngine` es el único motor en producción).
 */
enum class FeatureFlag {
    ACCESSIBILITY,
    OVERLAY,
    CAPTURE,
    PARSER,
    OCR,
    DETAILED_LOGS,
    METRICS,
    DEBUG_PANEL,
}
