package com.bhati.moodify.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhati.moodify.data.model.TrackDto

@Entity(tableName = "cached_tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val previewUrl: String?,
    val isLiked: Boolean = false,
    val searchType: String // "recommendation", "search", "liked"
)

fun TrackEntity.toDto() = TrackDto(
    id = id,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    previewUrl = previewUrl
)

fun TrackDto.toEntity(isLiked: Boolean, searchType: String) = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    previewUrl = previewUrl,
    isLiked = isLiked,
    searchType = searchType
)
