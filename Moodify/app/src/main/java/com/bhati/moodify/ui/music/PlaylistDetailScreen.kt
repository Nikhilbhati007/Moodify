package com.bhati.moodify.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    playlistName: String,
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val tracks by viewModel.currentPlaylistTracks.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.fetchPlaylistTracks(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("This playlist is empty", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks) { track ->
                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.removeTrackFromPlaylist(playlistId, track.id)
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
                                    modifier = Modifier.fillMaxSize().background(color).padding(16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
                                    }
                                }
                            },
                            content = {
                                TrackItem(
                                    track = track,
                                    isLiked = false, // We'd need to check liked status
                                    isPlaying = false,
                                    showPause = false,
                                    onClick = { viewModel.playTrack(track) },
                                    onLikeClick = { viewModel.toggleLike(track) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
