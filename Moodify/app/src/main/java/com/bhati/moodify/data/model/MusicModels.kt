package com.bhati.moodify.data.model

import com.google.gson.annotations.SerializedName

data class TrackDto(
    @SerializedName("trackId") val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String? = null,
    val previewUrl: String? = null
)

data class RecommendationResponse(
    val mood_detected: String,
    val recommendations: List<TrackDto>
)

data class LikeResponse(
    val message: String,
    val status: String // "added" or "removed"
)
