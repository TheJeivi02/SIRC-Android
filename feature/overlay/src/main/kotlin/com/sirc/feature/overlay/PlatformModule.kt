package com.sirc.feature.overlay

import com.sirc.core.platform.OfferDetectionEngine
import com.sirc.core.platform.OfferParserOrchestrator
import com.sirc.core.platform.OfferTypeParser
import com.sirc.core.platform.UberMotoParser
import com.sirc.core.platform.UberRadarParser
import com.sirc.core.platform.UberRequestParser
import com.sirc.core.platform.UberReservationParser
import com.sirc.core.platform.UberXlParser
import com.sirc.domain.engine.RuleEngine
import com.sirc.domain.model.OfferRule
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

    @Provides
    @Singleton
    fun provideRuleEngine(rules: List<@JvmSuppressWildcards OfferRule>): RuleEngine = RuleEngine(rules = rules)

    @Provides
    @Singleton
    fun provideOfferRules(): List<OfferRule> = RuleEngine.defaultRules()
}
