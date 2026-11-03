package com.bhati.moodify.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryDao {
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 100")
    suspend fun getPlayHistory(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(historyEntity: HistoryEntity)
    
    @Query("DELETE FROM play_history")
    suspend fun clearHistory()
}
