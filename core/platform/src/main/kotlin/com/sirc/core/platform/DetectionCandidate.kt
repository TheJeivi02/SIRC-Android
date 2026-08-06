package com.sirc.core.platform

/**
 * Candidato de la etapa de detección por keywords: el descriptor evaluado, la
 * pantalla detectada y su [matchScore] (diagnóstico).
 */
data class DetectionCandidate(
    val descriptor: PlatformDescriptor,
    val screenDetection: ScreenDetection,
    val matchScore: Int,
)
