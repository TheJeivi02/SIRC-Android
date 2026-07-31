package com.sirc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sirc.data.local.entity.OverlayConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OverlayConfigDao {
    @Query("SELECT * FROM overlay_config WHERE id = 1")
    fun observeConfig(): Flow<OverlayConfigEntity?>

    @Query("SELECT * FROM overlay_config WHERE id = 1")
    suspend fun getConfig(): OverlayConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: OverlayConfigEntity)
}
