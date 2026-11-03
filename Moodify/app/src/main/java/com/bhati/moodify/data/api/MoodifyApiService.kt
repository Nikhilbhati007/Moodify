package com.bhati.moodify.data.api

import com.bhati.moodify.data.model.AuthResponse
import com.bhati.moodify.data.model.LoginRequest
import com.bhati.moodify.data.model.RegisterRequest
import com.bhati.moodify.data.model.PlaylistDto
import com.bhati.moodify.data.model.CreatePlaylistRequest
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface MoodifyApiService {

    // ---- SUPABASE AUTHENTICATION (GoTrue) ----

    @POST("auth/v1/signup")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    // ---- SUPABASE DATABASE (PostgREST) ----

    // Liked Songs
    @GET("rest/v1/liked_songs")
    suspend fun getLikedSongs(): Response<List<SupabaseLikedSong>>

    @POST("rest/v1/liked_songs")
    suspend fun likeSong(
        @Body song: SupabaseLikedSong
    ): Response<Unit>

    @DELETE("rest/v1/liked_songs")
    suspend fun unlikeSong(
        @Query("track_id") trackIdFilter: String // e.g. "eq.TRACK_ID"
    ): Response<Unit>

    // Playlists
    @GET("rest/v1/playlists")
    suspend fun getPlaylists(): Response<List<PlaylistDto>>

    // Create playlist. Supabase will return the created object in the body if we send Prefer header representation
    @Headers("Prefer: return=representation")
    @POST("rest/v1/playlists")
    suspend fun createPlaylist(
        @Body request: CreatePlaylistRequest
    ): Response<PlaylistDto>

    // Playlist Tracks
    @GET("rest/v1/playlist_tracks")
    suspend fun getPlaylistTracks(
        @Query("playlist_id") playlistIdFilter: String // e.g. "eq.PLAYLIST_ID"
    ): Response<List<SupabasePlaylistTrack>>

    @POST("rest/v1/playlist_tracks")
    suspend fun addTrackToPlaylist(
        @Body track: SupabasePlaylistTrack
    ): Response<Unit>

    @DELETE("rest/v1/playlist_tracks")
    suspend fun removeTrackFromPlaylist(
        @Query("playlist_id") playlistIdFilter: String, // e.g. "eq.PLAYLIST_ID"
        @Query("track_id") trackIdFilter: String      // e.g. "eq.TRACK_ID"
    ): Response<Unit>
}

// ---- SUPABASE DATA TRANSFER OBJECTS (DTOs) ----

data class SupabaseLikedSong(
    @SerializedName("track_id") val trackId: String,
    val title: String,
    val artist: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("preview_url") val previewUrl: String?
)

data class SupabasePlaylistTrack(
    @SerializedName("playlist_id") val playlistId: Int,
    @SerializedName("track_id") val trackId: String,
    val title: String,
    val artist: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("preview_url") val previewUrl: String?
)
