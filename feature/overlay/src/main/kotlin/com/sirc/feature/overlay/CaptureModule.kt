package com.sirc.feature.overlay

import com.sirc.capture.android.MediaProjectionCaptureInput
import com.sirc.capture.cache.CaptureFrameCache
import com.sirc.capture.cache.InMemoryCaptureFrameCache
import com.sirc.capture.di.AccessibilityRequests
import com.sirc.capture.di.CaptureRequests
import com.sirc.capture.flag.FeatureFlags
import com.sirc.capture.flag.InMemoryFeatureFlags
import com.sirc.capture.log.SircLogger
import com.sirc.capture.metrics.InMemoryOfferPerformanceTracker
import com.sirc.capture.metrics.OfferPerformanceTracker
import com.sirc.capture.model.CaptureRequest
import com.sirc.capture.observer.WindowObserver
import com.sirc.capture.ocr.OcrEngine
import com.sirc.capture.parser.OfferParser
import com.sirc.capture.parser.PlatformOfferParser
import com.sirc.capture.pipeline.CapturePipeline
import com.sirc.capture.pipeline.DefaultCapturePipeline
import com.sirc.capture.repository.CaptureRepository
import com.sirc.capture.repository.InMemoryCaptureRepository
import com.sirc.domain.repository.OfferEvaluationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
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
    abstract fun bindWindowEventPublisher(impl: AccessibilityWindowObserver): WindowEventPublisher

    @Binds
    @Singleton
    abstract fun bindFeatureFlags(impl: InMemoryFeatureFlags): FeatureFlags

    @Binds
    @Singleton
    abstract fun bindOfferParser(impl: PlatformOfferParser): OfferParser

    @Binds
    @Singleton
    abstract fun bindCaptureRepository(impl: InMemoryCaptureRepository): CaptureRepository

    @Binds
    @Singleton
    abstract fun bindSircLogger(impl: AndroidSircLogger): SircLogger

    @Binds
    @Singleton
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine

    @Binds
    @Singleton
    abstract fun bindCapturePipeline(impl: DefaultCapturePipeline): CapturePipeline

    @Binds
    @Singleton
    abstract fun bindCaptureFrameCache(impl: InMemoryCaptureFrameCache): CaptureFrameCache

    @Binds
    @Singleton
    abstract fun bindOfferPerformanceTracker(impl: InMemoryOfferPerformanceTracker): OfferPerformanceTracker

    @Binds
    @Singleton
    abstract fun bindOfferEvaluationRepository(impl: InMemoryOfferEvaluationRepository): OfferEvaluationRepository
}

/** Providers de las corrientes de [CaptureRequest] cualificadas (WP-E3-03). */
@Module
@InstallIn(SingletonComponent::class)
object CaptureFlowsModule {
    @Provides
    @Singleton
    @AccessibilityRequests
    fun provideAccessibilityRequests(input: AccessibilityCaptureInput): Flow<CaptureRequest> = input.requests()

    @Provides
    @Singleton
    @CaptureRequests
    fun provideCaptureRequests(input: MediaProjectionCaptureInput): Flow<CaptureRequest> = input.requests()
}
