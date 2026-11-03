package com.bhati.moodify.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntity)

    @Query("SELECT * FROM moods ORDER BY timestamp DESC")
    suspend fun getAllMoods(): List<MoodEntity>

    @Query("SELECT COUNT(*) FROM moods WHERE mood = :mood")
    suspend fun getMoodCount(mood: String): Int
}
