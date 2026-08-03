package com.sirc.feature.overlay

import com.sirc.core.platform.OfferDetectionEngine
import com.sirc.core.platform.OfferParserOrchestrator
import com.sirc.core.platform.OfferTypeParser
import com.sirc.core.platform.UberMotoParser
import com.sirc.core.platform.UberRadarParser
import com.sirc.core.platform.UberRequestParser
import com.sirc.core.platform.UberReservationParser
import com.sirc.core.platform.UberXlParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings del motor de análisis de texto ([com.sirc.core.platform]).
 *
 * Provee explícitamente el [OfferDetectionEngine], el conjunto de parsers
 * especializados y el [OfferParserOrchestrator] que usa el pipeline de
 * captura para parsear pantallas reales.
 *
 * WP-E1-02: `RuleEngine` quedó fuera de la ruta de producción;
 * `ProfitEngine` es el único motor de decisión. Los providers y providers de
 * reglas (provideRuleEngine/provideOfferRules) se eliminaron. La clase
 * `RuleEngine` en `:domain` se marca como LEGACY para uso en tests futuros.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides
    @Singleton
    fun provideOfferDetectionEngine(): OfferDetectionEngine = OfferDetectionEngine()

    @Provides
    @Singleton
    fun provideOfferTypeParsers(): List<OfferTypeParser> =
        listOf(
            UberMotoParser(),
            UberXlParser(),
            UberRadarParser(),
            UberReservationParser(),
            UberRequestParser(),
        )

    @Provides
    @Singleton
    fun provideOfferParserOrchestrator(
        detectionEngine: OfferDetectionEngine,
        parsers: List<@JvmSuppressWildcards OfferTypeParser>,
    ): OfferParserOrchestrator =
        OfferParserOrchestrator(
            detectionEngine = detectionEngine,
            specializedParsers = parsers,
        )
}
