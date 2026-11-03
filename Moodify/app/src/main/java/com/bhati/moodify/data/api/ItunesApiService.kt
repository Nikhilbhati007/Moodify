package com.bhati.moodify.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApiService {
    @GET("search")
    suspend fun searchSongs(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int = 0,
        @Query("country") country: String? = null
    ): Response<ItunesResponse>
}

data class ItunesResponse(
    val resultCount: Int,
    val results: List<ItunesTrack>
)

data class ItunesTrack(
    val trackId: Long,
    val trackName: String?,
    val artistName: String?,
    val artworkUrl100: String?,
    val previewUrl: String?
)
