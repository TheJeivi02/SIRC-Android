package com.sirc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sirc.data.local.entity.OfferHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferHistoryDao {
    @Query("SELECT * FROM offer_history ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeEntries(limit: Int): Flow<List<OfferHistoryEntity>>

    @Insert
    suspend fun insert(entry: OfferHistoryEntity)

    @Query("DELETE FROM offer_history")
    suspend fun clear()

    @Query(
        "DELETE FROM offer_history WHERE id NOT IN (" +
            "SELECT id FROM offer_history ORDER BY timestampMillis DESC LIMIT :limit)",
    )
    suspend fun trimToLimit(limit: Int)

    @Query("SELECT COUNT(*) FROM offer_history")
    suspend fun count(): Int
}
