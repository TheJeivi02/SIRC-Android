package com.sirc.data.di

import android.content.Context
import androidx.room.Room
import com.sirc.data.local.SircDatabase
import com.sirc.data.local.SircMigrations
import com.sirc.data.local.dao.DriverConfigDao
import com.sirc.data.local.dao.OfferHistoryDao
import com.sirc.data.local.dao.OverlayConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SircDatabase =
        Room.databaseBuilder(
            context,
            SircDatabase::class.java,
            "sirc.db",
        ).addMigrations(
            SircMigrations.MIGRATION_1_2,
            SircMigrations.MIGRATION_2_3,
            SircMigrations.MIGRATION_3_4,
        ).build()

    @Provides
    fun provideDriverConfigDao(db: SircDatabase): DriverConfigDao = db.driverConfigDao()

    @Provides
    fun provideOverlayConfigDao(db: SircDatabase): OverlayConfigDao = db.overlayConfigDao()

    @Provides
    fun provideOfferHistoryDao(db: SircDatabase): OfferHistoryDao = db.offerHistoryDao()
}
