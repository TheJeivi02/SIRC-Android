package com.sirc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirc.data.local.entity.DriverConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverConfigDao {
    @Query("SELECT * FROM driver_config WHERE id = 1")
    fun observeConfig(): Flow<DriverConfigEntity?>

    @Query("SELECT * FROM driver_config WHERE id = 1")
    suspend fun getConfig(): DriverConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: DriverConfigEntity)
}
