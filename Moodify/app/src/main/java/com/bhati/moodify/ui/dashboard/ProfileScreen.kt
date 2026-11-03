package com.bhati.moodify.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhati.moodify.ui.music.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    username: String?,
    musicViewModel: MusicViewModel,
    onLogoutClick: () -> Unit,
    onBack: () -> Unit
) {
    val moodStats by musicViewModel.moodStats.collectAsState()

    LaunchedEffect(Unit) {
        musicViewModel.fetchMoodStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DB954)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username?.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = username ?: "User", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(48.dp))

            // Analytics Section
            Text(
                "Your Vibe Analytics",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (moodStats.isEmpty()) {
                Text("No vibe data collected yet. Scan your face to start!", color = Color.Gray)
            } else {
                moodStats.forEach { (mood, percentage) ->
                    val color = when (mood.lowercase()) {
                        "happy" -> Color(0xFF00B4DB)
                        "sad" -> Color(0xFF6A82FB)
                        "energetic" -> Color(0xFFFF416C)
                        "calm" -> Color(0xFF00C9FF)
                        else -> Color.Gray
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mood, color = Color.White)
                            Text("${(percentage * 100).toInt()}%", color = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = color,
                            trackColor = Color(0xFF282828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
