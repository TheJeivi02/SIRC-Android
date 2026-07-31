package com.sirc.data.di

import com.sirc.data.repository.DefaultDriverConfigRepository
import com.sirc.data.repository.DefaultOfferHistoryRepository
import com.sirc.data.repository.DefaultOverlayConfigRepository
import com.sirc.domain.repository.DriverConfigRepository
import com.sirc.domain.repository.OfferHistoryRepository
import com.sirc.domain.repository.OverlayConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDriverConfigRepository(impl: DefaultDriverConfigRepository): DriverConfigRepository

    @Binds
    @Singleton
    abstract fun bindOverlayConfigRepository(impl: DefaultOverlayConfigRepository): OverlayConfigRepository

    @Binds
    @Singleton
    abstract fun bindOfferHistoryRepository(impl: DefaultOfferHistoryRepository): OfferHistoryRepository
}
