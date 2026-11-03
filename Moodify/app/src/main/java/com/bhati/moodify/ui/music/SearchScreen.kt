package com.bhati.moodify.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val focusManager = LocalFocusManager.current
    var isPlayerExpanded by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var trackToAddToPlaylist by remember { mutableStateOf<com.bhati.moodify.data.model.TrackDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchPlaylists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212) // Spotify Dark
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("What do you want to listen to?", color = Color.Gray) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchTracks(searchQuery)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF282828),
                        unfocusedContainerColor = Color(0xFF282828),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // Results Container
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    when (val state = uiState) {
                        is MusicState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                // Initially loading can be empty until a search is made, or a loader
                                if (searchQuery.isNotBlank()) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        is MusicState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Oops: ${state.message}", color = Color.White)
                            }
                        }
                        is MusicState.Success -> {
                            if (state.tracks.isEmpty() && searchQuery.isNotBlank()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No results found.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 100.dp) // Leave space for mini-player
                                ) {
                                    items(state.tracks) { track ->
                                        val isLiked = state.likedTrackIds.contains(track.id)
                                        TrackItem(
                                            track = track,
                                            isLiked = isLiked,
                                            isPlaying = currentTrack?.id == track.id,
                                            onClick = { 
                                                focusManager.clearFocus()
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                viewModel.playTrack(track) 
                                            },
                                            onLikeClick = { 
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                viewModel.toggleLike(track) 
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                trackToAddToPlaylist = track
                                            },
                                            showPause = currentTrack?.id == track.id && isPlaying
                                        )
                                    }
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(
                                                onClick = { 
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                    viewModel.searchTracks(searchQuery, isLoadMore = true) 
                                                }
                                            ) {
                                                Text("Show More...", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (trackToAddToPlaylist != null) {
                    PlaylistSelectionDialog(
                        track = trackToAddToPlaylist!!,
                        viewModel = viewModel,
                        onDismiss = { trackToAddToPlaylist = null }
                    )
                }
            }

            // Bottom Mini Player
            if (currentTrack != null) {
                MiniPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.togglePlayback() 
                    },
                    onClick = { isPlayerExpanded = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            if (currentTrack != null) {
                FullScreenPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.togglePlayback() 
                    },
                    onClose = { isPlayerExpanded = false },
                    lyricsState = lyricsState,
                    viewModel = viewModel
                )
            }
        }
    }
}
