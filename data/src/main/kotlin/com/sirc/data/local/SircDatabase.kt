package com.sirc.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sirc.data.local.dao.DriverConfigDao
import com.sirc.data.local.dao.OfferHistoryDao
import com.sirc.data.local.dao.OverlayConfigDao
import com.sirc.data.local.entity.DriverConfigEntity
import com.sirc.data.local.entity.OfferHistoryEntity
import com.sirc.data.local.entity.OverlayConfigEntity

@Database(
    entities = [
        DriverConfigEntity::class,
        OverlayConfigEntity::class,
        OfferHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class SircDatabase : RoomDatabase() {
    abstract fun driverConfigDao(): DriverConfigDao

    abstract fun overlayConfigDao(): OverlayConfigDao

    abstract fun offerHistoryDao(): OfferHistoryDao
}
