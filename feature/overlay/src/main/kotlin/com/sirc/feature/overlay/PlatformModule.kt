package com.sirc.feature.overlay

import com.sirc.core.platform.OfferParserOrchestrator
import com.sirc.core.platform.PlatformDescriptorRegistry
import com.sirc.core.platform.PlatformDescriptors
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings del motor de análisis de texto ([com.sirc.core.platform]).
 *
 * Provee el [PlatformDescriptorRegistry] (única fuente de descriptores de
 * plataforma, validados en construcción) y el [OfferParserOrchestrator] que usa
 * el pipeline de captura para parsear pantallas reales.
 *
 * WP-E3-01: se elimina la inyección de parsers especializados de Uber
 * (`provideOfferDetectionEngine`/`provideOfferTypeParsers`); el orquestador es
 * 100 % descriptor-driven y los parsers los construye el registry desde cada
 * descriptor.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides
    @Singleton
    fun providePlatformDescriptorRegistry(): PlatformDescriptorRegistry =
        PlatformDescriptorRegistry(PlatformDescriptors.all())

    @Provides
    @Singleton
    fun provideOfferParserOrchestrator(registry: PlatformDescriptorRegistry): OfferParserOrchestrator =
        OfferParserOrchestrator(registry)
}
