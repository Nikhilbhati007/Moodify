package com.bhati.moodify.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY id DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    suspend fun getTracksForPlaylist(playlistId: Int): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearTracksForPlaylist(playlistId: Int)
}
