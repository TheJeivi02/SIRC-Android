package com.sirc.capture.android

import com.sirc.capture.android.metrics.DebugCaptureMetrics
import com.sirc.capture.android.provider.MediaProjectionScreenCaptureProvider
import com.sirc.capture.android.provider.ScreenCaptureProvider
import com.sirc.capture.metrics.CaptureMetrics
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Bindings Android de la captura: MediaProjection y métricas. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureAndroidModule {
    @Binds
    @Singleton
    abstract fun bindScreenCaptureProvider(impl: MediaProjectionScreenCaptureProvider): ScreenCaptureProvider

    @Binds
    @Singleton
    abstract fun bindCaptureMetrics(impl: DebugCaptureMetrics): CaptureMetrics
}
