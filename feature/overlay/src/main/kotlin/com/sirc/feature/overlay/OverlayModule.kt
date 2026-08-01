package com.sirc.feature.overlay

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OverlayModule {
    @Binds
    @Singleton
    abstract fun bindPermissionManager(impl: AndroidPermissionManager): PermissionManager

    @Binds
    @Singleton
    abstract fun bindOverlayManager(impl: AndroidOverlayManager): OverlayManager

    @Binds
    @Singleton
    abstract fun bindOverlayDataSource(impl: PipelineOverlayDataSource): OverlayDataSource
}
