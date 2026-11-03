package com.bhati.moodify.data.model

data class PlaylistDto(
    val id: Int,
    val name: String,
    val mood_tag: String
)

data class CreatePlaylistRequest(
    val name: String,
    val mood_tag: String? = null
)
