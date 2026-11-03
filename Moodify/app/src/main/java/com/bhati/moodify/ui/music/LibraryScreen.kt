package com.bhati.moodify.ui.music

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val libraryTracks by viewModel.libraryTracks.collectAsState()
    val isRefreshing by viewModel.isLibraryRefreshing.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<com.bhati.moodify.data.model.TrackDto?>(null) }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.fetchLibraryTracks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liked Songs", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.fetchLibraryTracks()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            Column(modifier = Modifier.fillMaxSize()) {
                if (isRefreshing && libraryTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (libraryTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No liked songs yet. Go heart some tracks!", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(libraryTracks, key = { it.id }) { track ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleLike(track)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) 
                                        Color.Red.copy(alpha = 0.5f) else Color.Transparent
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.White)
                                        }
                                    }
                                },
                                content = {
                                    TrackItem(
                                        track = track,
                                        isLiked = true,
                                        isPlaying = currentTrack?.id == track.id,
                                        showPause = currentTrack?.id == track.id && isPlaying,
                                        onClick = { viewModel.playTrack(track) },
                                        onLikeClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleLike(track)
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            trackToAddToPlaylist = track
                                        }
                                    )
                                }
                            )
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

            // Bottom Player
            if (currentTrack != null) {
                MiniPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.togglePlayback() 
                    },
                    onClick = { isPlayerExpanded = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            if (currentTrack != null) {
                FullScreenPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
