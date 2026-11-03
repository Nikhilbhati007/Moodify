package com.bhati.moodify.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhati.moodify.data.model.TrackDto

@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
    @PrimaryKey val uuid: String,
    val playlistId: Int,
    val trackId: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val previewUrl: String?,
    val addedAt: Long
)

fun TrackDto.toPlaylistTrackEntity(playlistId: Int) = PlaylistTrackEntity(
    uuid = "${playlistId}_${id}",
    playlistId = playlistId,
    trackId = id,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    previewUrl = previewUrl,
    addedAt = System.currentTimeMillis()
)

fun PlaylistTrackEntity.toDto() = TrackDto(
    id = trackId,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    previewUrl = previewUrl
)
