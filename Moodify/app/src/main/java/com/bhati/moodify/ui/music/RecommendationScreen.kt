package com.bhati.moodify.ui.music

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bhati.moodify.data.model.TrackDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    mood: String,
    lang: String,
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackDto?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(mood, lang) {
        viewModel.fetchPlaylists() // Pre-fetch playlists for the dialog
        viewModel.fetchRecommendations(mood, lang)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Made for $mood") },
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
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Top Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                Color(0xFF121212)
                            )
                        )
                    )
            )

            // Content
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (val state = uiState) {
                    is MusicState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is MusicState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Oops: ${state.message}", color = Color.White)
                        }
                    }
                    is MusicState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(state.tracks) { track ->
                                val isLiked = state.likedTrackIds.contains(track.id)
                                TrackItem(
                                    track = track,
                                    isLiked = isLiked,
                                    isPlaying = currentTrack?.id == track.id,
                                    onClick = { 
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
                                            viewModel.fetchRecommendations(mood, lang, isLoadMore = true) 
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
            
            if (trackToAddToPlaylist != null) {
                PlaylistSelectionDialog(
                    track = trackToAddToPlaylist!!,
                    viewModel = viewModel,
                    onDismiss = { trackToAddToPlaylist = null }
                )
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
}

@Composable
fun MiniPlayer(
    track: TrackDto, 
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color(0xFF1E2A38)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E2A38), Color(0xFF0F1722))
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(track.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = track.artist,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = onPlayPauseToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 
                    contentDescription = "Play/Pause", 
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun FullScreenPlayer(
    track: TrackDto,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onClose: () -> Unit,
    lyricsState: MusicViewModel.LyricsState = MusicViewModel.LyricsState.Idle,
    viewModel: MusicViewModel
) {
    var dominantColor by remember { mutableStateOf(Color(0xFF1E2A38)) }
    val animatedColor by animateColorAsState(targetValue = dominantColor, label = "color")
    var showLyrics by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackDto?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedColor.copy(alpha = 0.8f), Color(0xFF121212))
                )
            )
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(track.imageUrl?.replace("100x100", "600x600") ?: "")
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                onSuccess = { success ->
                    val bitmap = (success.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    bitmap?.let {
                        androidx.palette.graphics.Palette.from(it).generate { palette ->
                            palette?.dominantSwatch?.rgb?.let { colorValue ->
                                dominantColor = Color(colorValue)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(300.dp)
                    .shadow(24.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = track.title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color.LightGray,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(32.dp))

            Slider(
                value = 0.3f, 
                onValueChange = {},
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1:05", color = Color.Gray, fontSize = 12.sp)
                Text("-2:14", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
                
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White).clickable { onPlayPauseToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 
                        contentDescription = "Play/Pause", 
                        tint = Color.Black,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { 
                    showLyrics = !showLyrics 
                    if(showLyrics) showAiMenu = false
                }) {
                    Text(if (showLyrics) "Hide Lyrics" else "Show Lyrics", color = Color.LightGray, fontSize = 16.sp)
                }

                TextButton(onClick = { 
                    if (!showAiMenu) viewModel.fetchAiSimilar(track)
                    showAiMenu = !showAiMenu 
                    if(showAiMenu) showLyrics = false
                }) {
                    Text("✨ Ask Grok", color = Color(0xFFA259FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        if (trackToAddToPlaylist != null) {
            PlaylistSelectionDialog(
                track = trackToAddToPlaylist!!,
                viewModel = viewModel,
                onDismiss = { trackToAddToPlaylist = null }
            )
        }

        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Lyrics", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showLyrics = false }) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close Lyrics", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (lyricsState) {
                        is MusicViewModel.LyricsState.Idle -> {}
                        is MusicViewModel.LyricsState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is MusicViewModel.LyricsState.Found -> {
                            LazyColumn {
                                item {
                                    Text(
                                        text = (lyricsState as MusicViewModel.LyricsState.Found).lyrics,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        lineHeight = 32.sp
                                    )
                                }
                            }
                        }
                        is MusicViewModel.LyricsState.NotFound -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text((lyricsState as MusicViewModel.LyricsState.NotFound).reason, color = Color.Gray, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAiMenu,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val aiState by viewModel.aiRecommendations.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("✨ Grok Similar", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAiMenu = false }) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (val state = aiState) {
                        is MusicState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFA259FF))
                            }
                        }
                        is MusicState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(state.message, color = Color.Gray, fontSize = 18.sp)
                            }
                        }
                        is MusicState.Success -> {
                            LazyColumn {
                                items(state.tracks) { aiTrack ->
                                    TrackItem(
                                        track = aiTrack,
                                        isLiked = state.likedTrackIds.contains(aiTrack.id),
                                        isPlaying = false,
                                        showPause = false,
                                        onClick = {
                                            viewModel.playTrack(aiTrack)
                                            showAiMenu = false
                                        },
                                        onLikeClick = { viewModel.toggleLike(aiTrack) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            trackToAddToPlaylist = aiTrack
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
