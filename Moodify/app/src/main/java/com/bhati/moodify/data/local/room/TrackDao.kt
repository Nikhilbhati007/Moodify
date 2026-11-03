package com.bhati.moodify.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackDao {
    @Query("SELECT * FROM cached_tracks WHERE searchType = :searchType")
    suspend fun getTracksByType(searchType: String): List<TrackEntity>

    @Query("SELECT * FROM cached_tracks WHERE isLiked = 1")
    suspend fun getLikedTracks(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM cached_tracks WHERE searchType = :searchType")
    suspend fun clearTracksByType(searchType: String)

    @Query("UPDATE cached_tracks SET isLiked = :isLiked WHERE id = :trackId")
    suspend fun updateLikeStatus(trackId: String, isLiked: Boolean)
    
    @Query("DELETE FROM cached_tracks")
    suspend fun clearAll()
}
