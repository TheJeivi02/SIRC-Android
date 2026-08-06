package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform

/**
 * Registro de descriptores de plataforma: única fuente de verdad de qué
 * plataformas están soportadas y cómo analizarlas.
 *
 * Resuelve por plataforma o por paquete, y expone componentes precompilados e
 * inmutables por plataforma ([OfferDetectionEngine], [GenericPlatformExtractor]
 * y parsers de variantes) para que el parseo no resuelva ni valide nada por
 * frame.
 *
 * Valida los descriptores **en construcción** (falla rápido): cualquier
 * descriptor inválido lanza [IllegalArgumentException] al instanciar el
 * registry, nunca durante el parseo.
 */
class PlatformDescriptorRegistry(
    descriptors: List<PlatformDescriptor>,
) {
    private val descriptorsByPlatform: Map<RidePlatform, PlatformDescriptor>
    private val descriptorsByPackageName: Map<String, PlatformDescriptor>
    private val detectionEngines: Map<RidePlatform, OfferDetectionEngine>
    private val extractors: Map<RidePlatform, GenericPlatformExtractor>
    private val variantParsers: Map<RidePlatform, List<OfferTypeParser>>

    init {
        validate(descriptors)
        descriptorsByPlatform = descriptors.associateBy { it.platform }
        descriptorsByPackageName =
            descriptors.flatMap { descriptor ->
                descriptor.packageNames.map { packageName -> packageName to descriptor }
            }.toMap()
        detectionEngines =
            descriptors.associate { descriptor ->
                descriptor.platform to OfferDetectionEngine(descriptor.detectionRules)
            }
        extractors =
            descriptors.associate { descriptor ->
                val extractor =
                    GenericPlatformExtractor(
                        descriptor.platform,
                        descriptor.extractorKeywords,
                        defaultCurrency = descriptor.defaultCurrency,
                    )
                descriptor.platform to extractor
            }
        variantParsers =
            descriptors.associate { descriptor ->
                val platformExtractor = extractors.getValue(descriptor.platform)
                val parsers =
                    descriptor.offerTypes.map { variant ->
                        GenericOfferTypeParser(variant, platformExtractor)
                    }
                descriptor.platform to parsers
            }
    }

    fun descriptorFor(platform: RidePlatform): PlatformDescriptor? = descriptorsByPlatform[platform]

    fun descriptorForPackageName(packageName: String): PlatformDescriptor? = descriptorsByPackageName[packageName]

    fun detectionEngineFor(platform: RidePlatform): OfferDetectionEngine? = detectionEngines[platform]

    fun extractorFor(platform: RidePlatform): GenericPlatformExtractor? = extractors[platform]

    fun variantParsersFor(platform: RidePlatform): List<OfferTypeParser> = variantParsers[platform].orEmpty()

    private fun validate(descriptors: List<PlatformDescriptor>) {
        val duplicates = descriptors.groupBy { it.platform }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "plataformas duplicadas en registry: $duplicates" }

        val dupAliases =
            descriptors.flatMap { d -> d.packageNames.map { pkg -> pkg to d.platform } }
                .groupBy { it.first }
                .filterValues { it.size > 1 }
                .keys
        require(dupAliases.isEmpty()) { "aliases de paquete duplicados en registry: $dupAliases" }

        descriptors.forEach { d ->
            require(d.packageNames.isNotEmpty()) { "${d.platform}: sin packageNames" }
            require(d.packageNames.all { it.isNotBlank() }) { "${d.platform}: packageNames inválidos" }
            require(d.detectionRules.isNotEmpty()) { "${d.platform}: reglas de detección vacías" }
            d.detectionRules.forEach { rule ->
                require(rule.keywords.isNotEmpty()) { "${d.platform}: regla ${rule.type} sin keywords" }
            }
            d.offerTypes.forEach { variant ->
                require(variant.keywords.isNotEmpty()) { "${d.platform}: variante ${variant.type} sin keywords" }
            }
            require(d.extractorKeywords.totalKeywords.isNotEmpty() || d.extractorKeywords.fareKeywords.isNotEmpty()) {
                "${d.platform}: extractor sin keywords"
            }
            require(CURRENCY_CODE.matches(d.defaultCurrency)) { "${d.platform}: moneda inválida ${d.defaultCurrency}" }
            require(d.detectionRules.any { it.type == ScreenType.REQUEST }) {
                "${d.platform}: descriptor inconsistente: sin regla de detección REQUEST"
            }
            val dupOfferTypes = d.offerTypes.groupBy { it.type }.filterValues { it.size > 1 }.keys
            require(dupOfferTypes.isEmpty()) { "${d.platform}: tipos de oferta duplicados: $dupOfferTypes" }
        }
    }

    companion object {
        private val CURRENCY_CODE = Regex("[A-Z]{3}")
    }
}
