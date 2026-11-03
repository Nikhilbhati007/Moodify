package com.bhati.moodify.ui.music

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun HistoryScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val playHistory by viewModel.playHistory.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val libraryTracks by viewModel.libraryTracks.collectAsState()
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Play History", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.fetchHistory()
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
                if (playHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No history yet. Start playing some music!", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playHistory, key = { it.id + "_" + System.identityHashCode(it) }) { track ->
                            TrackItem(
                                track = track,
                                isLiked = libraryTracks.any { it.id == track.id },
                                isPlaying = currentTrack?.id == track.id,
                                showPause = currentTrack?.id == track.id && isPlaying,
                                onClick = { viewModel.playTrack(track) },
                                onLikeClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleLike(track)
                                }
                            )
                        }
                    }
                }
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
