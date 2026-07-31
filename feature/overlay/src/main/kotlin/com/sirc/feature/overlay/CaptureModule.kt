package com.sirc.feature.overlay

import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.flag.InMemoryFeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.observer.WindowObserver
import com.sirc.capture.parser.FakeParser
import com.sirc.capture.parser.OfferParser
import com.sirc.capture.repository.CaptureRepository
import com.sirc.capture.repository.InMemoryCaptureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Bindings de la infraestructura de captura. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {
    @Binds
    @Singleton
    abstract fun bindWindowObserver(impl: AccessibilityWindowObserver): WindowObserver

    @Binds
    @Singleton
    abstract fun bindFeatureFlags(impl: InMemoryFeatureFlags): FeatureFlags

    @Binds
    @Singleton
    abstract fun bindOfferParser(impl: FakeParser): OfferParser

    @Binds
    @Singleton
    abstract fun bindCaptureRepository(impl: InMemoryCaptureRepository): CaptureRepository

    @Binds
    @Singleton
    abstract fun bindSircLogger(impl: AndroidSircLogger): SircLogger
}
