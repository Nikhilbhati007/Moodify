package com.bhati.moodify.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhati.moodify.data.model.TrackDto

@Entity(tableName = "play_history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val previewUrl: String?,
    val playedAt: Long = System.currentTimeMillis()
)

fun HistoryEntity.toDto() = TrackDto(
    id = id,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    previewUrl = previewUrl
)

fun TrackDto.toHistoryEntity() = HistoryEntity(
    id = id,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    previewUrl = previewUrl,
    playedAt = System.currentTimeMillis()
)
