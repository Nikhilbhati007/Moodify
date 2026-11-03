package com.bhati.moodify.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhati.moodify.data.model.PlaylistDto

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val mood_tag: String
)

fun PlaylistEntity.toDto() = PlaylistDto(
    id = id,
    name = name,
    mood_tag = mood_tag
)

fun PlaylistDto.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    mood_tag = mood_tag
)
