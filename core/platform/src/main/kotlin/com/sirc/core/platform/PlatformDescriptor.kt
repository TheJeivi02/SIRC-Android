package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform

/**
 * Descriptor de plataforma: reúne todo lo que el motor de análisis necesita
 * para detectar pantallas y extraer ofertas de una plataforma concreta.
 *
 * Estructura preparada para evolucionar sin romper la API pública. Cada sección
 * de datos mapea a un futuro subdescriptor:
 *
 * - [platform] y [packageNames]          → `IdentityDescriptor`
 * - [detectionRules]                     → `DetectionDescriptor`
 * - [offerTypes]                         → `OfferTypeDescriptor`
 * - [extractorKeywords] y [defaultCurrency] → `ExtractionDescriptor`
 * - (nombres/idioma localizados)         → `LocalizationDescriptor`
 *
 * Los subdescriptores NO se implementan todavía: el descriptor sigue siendo una
 * sola clase de datos cuyas secciones se pueden extraer de forma aditiva
 * (nuevos campos con valores por defecto) sin afectar a los consumidores
 * actuales. La validación vive en [PlatformDescriptorRegistry], que falla en
 * construcción ante descriptores inválidos.
 */
data class PlatformDescriptor(
    /** Identidad de la plataforma (→ IdentityDescriptor). */
    val platform: RidePlatform,
    /** Paquetes de la app de la plataforma (canónico + aliases) (→ IdentityDescriptor). */
    val packageNames: List<String> = listOf(platform.packageName),
    /** Reglas de detección de pantalla por plataforma (→ DetectionDescriptor). */
    val detectionRules: List<DetectionRule>,
    /** Variantes de oferta en orden de especificidad (→ OfferTypeDescriptor). */
    val offerTypes: List<OfferTypeVariant> = emptyList(),
    /** Keywords de extracción del total/tarifa (→ ExtractionDescriptor). */
    val extractorKeywords: PlatformKeywords,
    /** Moneda por defecto cuando el monto no la indica (→ ExtractionDescriptor). */
    val defaultCurrency: String,
)
