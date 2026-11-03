package com.bhati.moodify.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhati.moodify.data.api.RetrofitClient
import com.bhati.moodify.data.api.SupabaseLikedSong
import com.bhati.moodify.data.api.SupabasePlaylistTrack
import com.bhati.moodify.data.local.TokenManager
import com.bhati.moodify.data.local.room.TrackDao
import com.bhati.moodify.data.local.room.toDto
import com.bhati.moodify.data.local.room.toEntity
import com.bhati.moodify.data.local.room.MoodDao
import com.bhati.moodify.data.local.room.MoodEntity
import com.bhati.moodify.data.local.room.HistoryDao
import com.bhati.moodify.data.local.room.toHistoryEntity
import com.bhati.moodify.data.local.room.PlaylistDao
import com.bhati.moodify.data.local.room.PlaylistEntity
import com.bhati.moodify.data.local.room.PlaylistTrackEntity
import com.bhati.moodify.data.local.room.toPlaylistTrackEntity
import com.bhati.moodify.data.model.PlaylistDto
import com.bhati.moodify.data.model.CreatePlaylistRequest
import com.bhati.moodify.data.model.TrackDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

sealed class MusicState {
    object Loading : MusicState()
    data class Success(val mood: String, val tracks: List<TrackDto>, val likedTrackIds: Set<String>, val isOffline: Boolean = false) : MusicState()
    data class Error(val message: String) : MusicState()
}

class MusicViewModel(
    private val tokenManager: TokenManager,
    private val trackDao: TrackDao,
    private val moodDao: MoodDao,
    private val historyDao: HistoryDao,
    private val playlistDao: PlaylistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicState>(MusicState.Loading)
    val uiState: StateFlow<MusicState> = _uiState.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackDto?>(null)
    val currentTrack: StateFlow<TrackDto?> = _currentTrack.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _libraryTracks = MutableStateFlow<List<TrackDto>>(emptyList())
    val libraryTracks: StateFlow<List<TrackDto>> = _libraryTracks.asStateFlow()

    private val _playHistory = MutableStateFlow<List<TrackDto>>(emptyList())
    val playHistory: StateFlow<List<TrackDto>> = _playHistory.asStateFlow()

    private val _isLibraryRefreshing = MutableStateFlow(false)
    val isLibraryRefreshing: StateFlow<Boolean> = _isLibraryRefreshing.asStateFlow()

    private val _moodStats = MutableStateFlow<Map<String, Float>>(emptyMap())
    val moodStats: StateFlow<Map<String, Float>> = _moodStats.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistDto>>(emptyList())
    val playlists: StateFlow<List<PlaylistDto>> = _playlists.asStateFlow()

    private val _currentPlaylistTracks = MutableStateFlow<List<TrackDto>>(emptyList())
    val currentPlaylistTracks: StateFlow<List<TrackDto>> = _currentPlaylistTracks.asStateFlow()

    sealed class LyricsState {
        object Idle : LyricsState()
        object Loading : LyricsState()
        data class Found(val lyrics: String) : LyricsState()
        data class NotFound(val reason: String) : LyricsState()
    }
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    private var currentRecommendationPage = 1
    private var currentSearchPage = 1

    private val _aiRecommendations = MutableStateFlow<MusicState>(MusicState.Loading)
    val aiRecommendations: StateFlow<MusicState> = _aiRecommendations.asStateFlow()

    fun fetchAiSimilar(track: TrackDto) {
        val token = tokenManager.getToken()
        if (token == null) {
            _aiRecommendations.value = MusicState.Error("Not authenticated")
            return
        }

        viewModelScope.launch {
            _aiRecommendations.value = MusicState.Loading
            try {
                // If a Supabase Edge Function is set up, it will fetch from functions/v1/ai-recommend
                // Otherwise this falls back to mock error state safely.
                val response = RetrofitClient.apiService.getPlaylists() // Simple heartbeat
                if (response.isSuccessful) {
                    _aiRecommendations.value = MusicState.Error("Supabase Serverless AI recommendation function not configured yet.")
                } else {
                    _aiRecommendations.value = MusicState.Error("Failed to fetch recommendations from Supabase Functions")
                }
            } catch (e: Exception) {
                _aiRecommendations.value = MusicState.Error("Edge Function not deployed: ${e.message}")
            }
        }
    }

    fun logMood(moodString: String) {
        viewModelScope.launch {
            moodDao.insertMood(MoodEntity(mood = moodString))
            fetchMoodStats()
        }
    }

    fun fetchLyrics(artist: String, title: String) {
        _lyricsState.value = LyricsState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanArtist = java.net.URLEncoder.encode(artist, "UTF-8").replace("+", "%20")
                val cleanTitle = java.net.URLEncoder.encode(title, "UTF-8").replace("+", "%20")
                val urlString = "https://api.lyrics.ovh/v1/$cleanArtist/$cleanTitle"
                
                val connection = URL(urlString).openConnection()
                connection.setRequestProperty("Accept", "application/json")
                val inputStream = connection.getInputStream()
                val response = inputStream.bufferedReader().use { it.readText() }
                
                val jsonResponse = org.json.JSONObject(response)
                val rawLyrics = jsonResponse.optString("lyrics", "")
                
                withContext(Dispatchers.Main) {
                    if (rawLyrics.isNotBlank()) {
                        _lyricsState.value = LyricsState.Found(rawLyrics)
                    } else {
                        _lyricsState.value = LyricsState.NotFound("Instrumental or Not Found.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _lyricsState.value = LyricsState.NotFound("Lyrics not available.")
                }
            }
        }
    }

    fun fetchMoodStats() {
        viewModelScope.launch {
            val allMoods = moodDao.getAllMoods()
            if (allMoods.isNotEmpty()) {
                val total = allMoods.size.toFloat()
                val counts = allMoods.groupingBy { it.mood }.eachCount()
                val percentages = counts.mapValues { it.value / total }
                _moodStats.value = percentages
            }
        }
    }

    fun fetchRecommendations(mood: String, lang: String, isLoadMore: Boolean = false) {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = MusicState.Error("Not authenticated")
            return
        }

        if (isLoadMore) {
            currentRecommendationPage++
        } else {
            currentRecommendationPage = 1
        }

        viewModelScope.launch {
            if (!isLoadMore) _uiState.value = MusicState.Loading
            try {
                // Formulate search term directly on the client (exact parity with server.js)
                val mappedMood = if (mood.equals("neutral", ignoreCase = true)) "Chill" else mood
                var searchTerm = "$lang $mappedMood song"
                val isIndicLang = listOf("hindi", "punjabi", "tamil", "telugu").contains(lang.lowercase())
                val countryParam = if (isIndicLang) "IN" else null

                if (lang.equals("hindi", ignoreCase = true)) {
                    searchTerm = "hindi $mappedMood bollywood"
                } else if (lang.equals("punjabi", ignoreCase = true)) {
                    searchTerm = "punjabi $mappedMood"
                }

                // Direct iTunes search query
                val recResponse = RetrofitClient.itunesApiService.searchSongs(
                    term = searchTerm,
                    limit = 25,
                    offset = (currentRecommendationPage - 1) * 25,
                    country = countryParam
                )

                // Supabase Liked Songs query
                val likeResponse = RetrofitClient.apiService.getLikedSongs()

                if (recResponse.isSuccessful && likeResponse.isSuccessful) {
                    var newTracks = recResponse.body()?.results?.map { track ->
                        TrackDto(
                            id = track.trackId.toString(),
                            title = track.trackName ?: "Unknown Title",
                            artist = track.artistName ?: "Unknown Artist",
                            imageUrl = track.artworkUrl100?.replace("100x100", "300x300"),
                            previewUrl = track.previewUrl
                        )
                    }?.filter { it.previewUrl != null } ?: emptyList()

                    // Storefront Fallback Search (if US search yielded < 5 results, search IN storefront)
                    if (countryParam == null && newTracks.size < 5) {
                        try {
                            val fallbackResponse = RetrofitClient.itunesApiService.searchSongs(
                                term = searchTerm,
                                limit = 25,
                                offset = (currentRecommendationPage - 1) * 25,
                                country = "IN"
                            )
                            val fallbackTracks = fallbackResponse.body()?.results?.map { track ->
                                TrackDto(
                                    id = track.trackId.toString(),
                                    title = track.trackName ?: "Unknown Title",
                                    artist = track.artistName ?: "Unknown Artist",
                                    imageUrl = track.artworkUrl100?.replace("100x100", "300x300"),
                                    previewUrl = track.previewUrl
                                )
                            }?.filter { it.previewUrl != null } ?: emptyList()

                            if (fallbackTracks.size > newTracks.size) {
                                newTracks = fallbackTracks
                            }
                        } catch (e: Exception) {}
                    }

                    val likedSongs = likeResponse.body() ?: emptyList()
                    val likedIds = likedSongs.map { it.trackId }.toSet()

                    val combinedTracks = if (isLoadMore && _uiState.value is MusicState.Success) {
                        (_uiState.value as MusicState.Success).tracks + newTracks
                    } else {
                        newTracks
                    }

                    saveToCache(combinedTracks, likedIds, "recommendation")
                    
                    _uiState.value = MusicState.Success(
                        mood = mood,
                        tracks = combinedTracks.distinctBy { it.id },
                        likedTrackIds = likedIds,
                        isOffline = false
                    )
                    
                    if (combinedTracks.isNotEmpty() && _currentTrack.value == null) {
                        _currentTrack.value = combinedTracks.first()
                    }
                } else {
                    loadFromCache("recommendation", "Supabase REST database connection error.")
                }
            } catch (e: Exception) {
                loadFromCache("recommendation", "Network error. Showing cached tracks.")
            }
        }
    }

    fun searchTracks(query: String, isLoadMore: Boolean = false) {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = MusicState.Error("Not authenticated")
            return
        }

        if (isLoadMore) {
            currentSearchPage++
        } else {
            currentSearchPage = 1
        }

        viewModelScope.launch {
            if (!isLoadMore) _uiState.value = MusicState.Loading
            try {
                // Auto-detect language parameter from search keywords (parity with server.js)
                var lang = "English"
                val qLower = query.lowercase()
                if (Regex("hindi|bollywood|arijit|shreya|kishore|rafi|lata|sonu|udit|sukhwinder|atif|darshan|jubin|mithoon|nehru").containsMatchIn(qLower)) {
                    lang = "Hindi"
                } else if (Regex("punjabi|punjab|diljit|sidhu|moosewala|aujla|dhillon|ap dhillon|bhangra|karan|shinda|amrinder|gippy|ikky|satisfya|imran").containsMatchIn(qLower)) {
                    lang = "Punjabi"
                } else if (Regex("tamil|anirudh|rahman|ilayaraja|yuvan|spb").containsMatchIn(qLower)) {
                    lang = "Tamil"
                } else if (Regex("telugu|keeravani|dsp|sid sriram|thaman").containsMatchIn(qLower)) {
                    lang = "Telugu"
                }

                val countryParam = if (listOf("hindi", "punjabi", "tamil", "telugu").contains(lang.lowercase())) "IN" else null

                // Search iTunes directly
                val searchResponse = RetrofitClient.itunesApiService.searchSongs(
                    term = query,
                    limit = 25,
                    offset = (currentSearchPage - 1) * 25,
                    country = countryParam
                )

                // Get liked songs from Supabase
                val likeResponse = RetrofitClient.apiService.getLikedSongs()

                if (searchResponse.isSuccessful && likeResponse.isSuccessful) {
                    var newTracks = searchResponse.body()?.results?.map { track ->
                        TrackDto(
                            id = track.trackId.toString(),
                            title = track.trackName ?: "Unknown Title",
                            artist = track.artistName ?: "Unknown Artist",
                            imageUrl = track.artworkUrl100?.replace("100x100", "300x300"),
                            previewUrl = track.previewUrl
                        )
                    }?.filter { it.previewUrl != null } ?: emptyList()

                    // Storefront Fallback Search
                    if (countryParam == null && newTracks.size < 5) {
                        try {
                            val fallbackResponse = RetrofitClient.itunesApiService.searchSongs(
                                term = query,
                                limit = 25,
                                offset = (currentSearchPage - 1) * 25,
                                country = "IN"
                            )
                            val fallbackTracks = fallbackResponse.body()?.results?.map { track ->
                                TrackDto(
                                    id = track.trackId.toString(),
                                    title = track.trackName ?: "Unknown Title",
                                    artist = track.artistName ?: "Unknown Artist",
                                    imageUrl = track.artworkUrl100?.replace("100x100", "300x300"),
                                    previewUrl = track.previewUrl
                                )
                            }?.filter { it.previewUrl != null } ?: emptyList()

                            if (fallbackTracks.size > newTracks.size) {
                                newTracks = fallbackTracks
                            }
                        } catch (e: Exception) {}
                    }

                    val likedSongs = likeResponse.body() ?: emptyList()
                    val likedIds = likedSongs.map { it.trackId }.toSet()
                    
                    val combinedTracks = if (isLoadMore && _uiState.value is MusicState.Success) {
                        (_uiState.value as MusicState.Success).tracks + newTracks
                    } else {
                        newTracks
                    }

                    saveToCache(combinedTracks, likedIds, "search")
                    
                    _uiState.value = MusicState.Success(
                        mood = "Search Results",
                        tracks = combinedTracks.distinctBy { it.id },
                        likedTrackIds = likedIds,
                        isOffline = false
                    )
                    
                    if (combinedTracks.isNotEmpty() && _currentTrack.value == null) {
                        _currentTrack.value = combinedTracks.first()
                    }
                } else {
                    loadFromCache("search", "Supabase REST database connection error.")
                }
            } catch (e: Exception) {
                loadFromCache("search", "Network error. Showing recent search.")
            }
        }
    }

    private suspend fun saveToCache(tracks: List<TrackDto>, likedIds: Set<String>, type: String) {
        trackDao.clearTracksByType(type)
        val entities = tracks.map { it.toEntity(likedIds.contains(it.id), type) }
        trackDao.insertTracks(entities)
    }

    private suspend fun loadFromCache(type: String, errorMsg: String) {
        val cached = trackDao.getTracksByType(type)
        if (cached.isNotEmpty()) {
            val tracks = cached.map { it.toDto() }
            val likedIds = cached.filter { it.isLiked }.map { it.id }.toSet()
            
            _uiState.value = MusicState.Success(
                mood = if(type == "search") "Cached Search" else "Cached Hits",
                tracks = tracks,
                likedTrackIds = likedIds,
                isOffline = true
            )
            if (_currentTrack.value == null) {
                _currentTrack.value = tracks.firstOrNull()
            }
        } else {
            _uiState.value = MusicState.Error(errorMsg)
        }
    }

    fun playTrack(track: TrackDto) {
        if (_currentTrack.value?.id == track.id && _isPlaying.value) {
            pausePlayback()
            return
        }

        _currentTrack.value = track
        
        viewModelScope.launch {
            historyDao.insertHistory(track.toHistoryEntity())
            fetchHistory()
        }
        
        fetchLyrics(track.artist, track.title)
        
        track.previewUrl?.let { url ->
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                try {
                    setDataSource(url)
                    setOnPreparedListener { 
                        start()
                        _isPlaying.value = true
                    }
                    setOnCompletionListener {
                        _isPlaying.value = false
                    }
                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("MusicViewModel", "MediaPlayer error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        true
                    }
                    prepareAsync()
                } catch (e: Exception) {
                    android.util.Log.e("MusicViewModel", "MediaPlayer setDataSource error", e)
                    _isPlaying.value = false
                }
            }
        }
    }
    
    fun togglePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                it.start()
                _isPlaying.value = true
            }
        }
    }
    
    fun pausePlayback() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun fetchHistory() {
        viewModelScope.launch {
            val entities = historyDao.getPlayHistory()
            _playHistory.value = entities.map { it.toDto() }
        }
    }

    fun fetchLibraryTracks() {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            _isLibraryRefreshing.value = true
            try {
                // Fetch direct from Supabase
                val response = RetrofitClient.apiService.getLikedSongs()
                if (response.isSuccessful) {
                    val tracks = response.body()?.map { track ->
                        TrackDto(
                            id = track.trackId,
                            title = track.title,
                            artist = track.artist,
                            imageUrl = track.imageUrl,
                            previewUrl = track.previewUrl
                        )
                    } ?: emptyList()
                    saveToCache(tracks, tracks.map { it.id }.toSet(), "liked")
                    _libraryTracks.value = tracks
                } else {
                    val likedEntities = trackDao.getLikedTracks()
                    val tracks = likedEntities.map { it.toDto() }.distinctBy { it.id }
                    _libraryTracks.value = tracks
                }
            } catch (e: Exception) {
                val likedEntities = trackDao.getLikedTracks()
                val tracks = likedEntities.map { it.toDto() }.distinctBy { it.id }
                _libraryTracks.value = tracks
            } finally {
                _isLibraryRefreshing.value = false
            }
        }
    }

    fun toggleLike(track: TrackDto) {
        val token = tokenManager.getToken() ?: return
        
        viewModelScope.launch {
            val currentState = _uiState.value
            
            val isCurrentlyLiked = _libraryTracks.value.any { it.id == track.id } || 
                (currentState is MusicState.Success && currentState.likedTrackIds.contains(track.id))
            
            if (currentState is MusicState.Success) {
                val updatedLikes = currentState.likedTrackIds.toMutableSet()
                if (isCurrentlyLiked) updatedLikes.remove(track.id) else updatedLikes.add(track.id)
                _uiState.value = currentState.copy(likedTrackIds = updatedLikes)
            }
            
            if (isCurrentlyLiked) {
                _libraryTracks.value = _libraryTracks.value.filter { it.id != track.id }
            } else {
                _libraryTracks.value = listOf(track) + _libraryTracks.value
            }

            try {
                trackDao.insertTracks(listOf(track.toEntity(!isCurrentlyLiked, "liked")))
                trackDao.updateLikeStatus(track.id, !isCurrentlyLiked)
                
                // Write/Delete direct on Supabase
                val response = if (isCurrentlyLiked) {
                    RetrofitClient.apiService.unlikeSong("eq.${track.id}")
                } else {
                    RetrofitClient.apiService.likeSong(
                        SupabaseLikedSong(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            imageUrl = track.imageUrl,
                            previewUrl = track.previewUrl
                        )
                    )
                }

                if (!response.isSuccessful) {
                    throw Exception("Supabase sync failed")
                }
                fetchLibraryTracks()
            } catch (e: Exception) {
                trackDao.updateLikeStatus(track.id, isCurrentlyLiked)
                fetchLibraryTracks()
                if (currentState is MusicState.Success) {
                    _uiState.value = currentState
                }
            }
        }
    }

    fun fetchPlaylists() {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            try {
                // GET direct from Supabase
                val response = RetrofitClient.apiService.getPlaylists()
                if (response.isSuccessful) {
                    val dtos = response.body() ?: emptyList()
                    playlistDao.clearPlaylists()
                    playlistDao.insertPlaylists(dtos.map { it.toEntity() })
                    _playlists.value = dtos
                } else {
                    _playlists.value = playlistDao.getAllPlaylists().map { it.toDto() }
                }
            } catch (e: Exception) {
                _playlists.value = playlistDao.getAllPlaylists().map { it.toDto() }
            }
        }
    }

    fun createPlaylist(name: String, moodTag: String? = null) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            try {
                // POST direct to Supabase
                val response = RetrofitClient.apiService.createPlaylist(CreatePlaylistRequest(name, moodTag ?: "Custom"))
                if (response.isSuccessful) {
                    fetchPlaylists()
                }
            } catch (e: Exception) {
                // handle error silently
            }
        }
    }

    fun fetchPlaylistTracks(playlistId: Int) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            _currentPlaylistTracks.value = emptyList()
            try {
                // GET direct from Supabase
                val response = RetrofitClient.apiService.getPlaylistTracks("eq.$playlistId")
                if (response.isSuccessful) {
                    val supabaseTracks = response.body() ?: emptyList()
                    val tracks = supabaseTracks.map { track ->
                        TrackDto(
                            id = track.trackId,
                            title = track.title,
                            artist = track.artist,
                            imageUrl = track.imageUrl,
                            previewUrl = track.previewUrl
                        )
                    }
                    playlistDao.clearTracksForPlaylist(playlistId)
                    playlistDao.insertPlaylistTracks(tracks.map { it.toPlaylistTrackEntity(playlistId) })
                    _currentPlaylistTracks.value = tracks
                } else {
                    _currentPlaylistTracks.value = playlistDao.getTracksForPlaylist(playlistId).map { it.toDto() }
                }
            } catch (e: Exception) {
                _currentPlaylistTracks.value = playlistDao.getTracksForPlaylist(playlistId).map { it.toDto() }
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Int, track: TrackDto) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            try {
                // POST direct to Supabase
                val response = RetrofitClient.apiService.addTrackToPlaylist(
                    SupabasePlaylistTrack(
                        playlistId = playlistId,
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        imageUrl = track.imageUrl,
                        previewUrl = track.previewUrl
                    )
                )
                if (response.isSuccessful) {
                    fetchPlaylistTracks(playlistId)
                }
            } catch (e: Exception) {
                // handle failure
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            try {
                // DELETE direct from Supabase
                val response = RetrofitClient.apiService.removeTrackFromPlaylist(
                    playlistIdFilter = "eq.$playlistId",
                    trackIdFilter = "eq.$trackId"
                )
                if (response.isSuccessful) {
                    fetchPlaylistTracks(playlistId)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
