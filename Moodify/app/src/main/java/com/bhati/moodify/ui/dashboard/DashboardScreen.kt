package com.bhati.moodify.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhati.moodify.ui.theme.ElectricPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    musicViewModel: com.bhati.moodify.ui.music.MusicViewModel,
    username: String?,
    onProfileClick: () -> Unit,
    onCameraClick: () -> Unit,
    onSearchClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onToggleTheme: () -> Unit,
    isDarkTheme: Boolean
) {
    val currentTrack by musicViewModel.currentTrack.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val lyricsState by musicViewModel.lyricsState.collectAsState()
    var isPlayerExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_anim"
    )

    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    val cardBackground = if (isDarkTheme) Color(0xFF203A43) else Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                title = { 
                    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val greeting = when (currentHour) {
                        in 0..11 -> "Good Morning"
                        in 12..16 -> "Good Afternoon"
                        else -> "Good Evening"
                    }
                    Column {
                        Text(greeting, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = textColor)
                        Text("Ready to vibe?", fontSize = 14.sp, color = secondaryTextColor)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onToggleTheme() 
                    }) {
                        Icon(
                            if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode, 
                            contentDescription = "Toggle Theme", 
                            tint = textColor
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        val firstLetter = username?.firstOrNull()?.uppercase() ?: "U"
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = firstLetter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp), // Adjust bottom padding for player
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

            // Beautiful glowing Floating Camera Scan Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary, 
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .clickable { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onCameraClick() 
                    }
            ) {
                Box(modifier = Modifier.size(150.dp).offset(x = (-40).dp, y = (-40).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
                Box(modifier = Modifier.size(200.dp).offset(x = 200.dp, y = 100.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Scan Your Face",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "We'll find the perfect music for your mood",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            // Search Bar Component
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(cardBackground)
                    .shadow(if (!isDarkTheme) 4.dp else 0.dp)
                    .clickable { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onSearchClick() 
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = textColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("What do you want to listen to?", color = secondaryTextColor, fontSize = 16.sp)
                }
            }

            // Horizontal Categories
            Column {
                Text("Vibe Categories", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val categories = listOf("Chill" to Color(0xFF00C9FF), "Focus" to Color(0xFFF2C94C), "Workout" to Color(
                        0xFFFF406C
                    ), "Happy" to Color(0xFF00B4DB))
                    items(categories.size) { index ->
                        val (title, color) = categories[index]
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color.copy(alpha = 0.8f))
                                .clickable { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onCategoryClick(title) 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Library Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LibraryCard("Liked Songs", Color(0xFF8A2BE2), onClick = onLibraryClick, modifier = Modifier.weight(1f))
                    LibraryCard("History", Color(0xFF2C5364), onClick = onHistoryClick, modifier = Modifier.weight(1f))
                }
                
                LibraryCard(
                    title = "My Playlists", 
                    color = Color(0xFF00B4DB), 
                    onClick = onPlaylistsClick, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Bottom Mini Player
    if (currentTrack != null) {
        com.bhati.moodify.ui.music.MiniPlayer(
            track = currentTrack!!,
            isPlaying = isPlaying,
            onPlayPauseToggle = { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                musicViewModel.togglePlayback() 
            },
            onClick = { isPlayerExpanded = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        if (currentTrack != null) {
            com.bhati.moodify.ui.music.FullScreenPlayer(
                track = currentTrack!!,
                isPlaying = isPlaying,
                onPlayPauseToggle = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    musicViewModel.togglePlayback() 
                },
                onClose = { isPlayerExpanded = false },
                lyricsState = lyricsState,
                viewModel = musicViewModel
            )
        }
    }
}
}

@Composable
fun LibraryCard(title: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(color, color.copy(alpha = 0.5f))
                )
            )
            .clickable { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick() 
            }
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize().offset(x = 30.dp, y = (-20).dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
