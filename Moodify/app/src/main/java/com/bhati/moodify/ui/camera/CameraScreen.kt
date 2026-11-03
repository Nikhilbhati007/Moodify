package com.bhati.moodify.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bhati.moodify.ml.FaceAnalyzer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onMoodCaptured: (mood: String, lang: String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (!cameraPermissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF191414)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission required", color = Color.White)
            Button(
                onClick = { cameraPermissionState.launchPermissionRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Grant Permission")
            }
        }
    } else {
        CameraPreviewWithAnalysis(onMoodCaptured)
    }
}

@Composable
fun CameraPreviewWithAnalysis(onMoodCaptured: (String, String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Core state
    var detectedMood by remember { mutableStateOf("Neutral") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var faceDetected by remember { mutableStateOf(false) }
    
    // Manual Override State
    var manualModeEnabled by remember { mutableStateOf(false) }
    val moodsList = listOf("Happy", "Sad", "Neutral", "Energetic", "Calm", "Romantic", "Party")

    // Dynamic Color based on Mood & Face Detection status
    val glowColor by animateColorAsState(
        targetValue = if (!manualModeEnabled && !faceDetected) {
            Color(0xFFFF3366) // Pulse Pinkish-Red when searching/scanning for face
        } else {
            when (detectedMood) {
                "Happy" -> Color(0xFFFFD700) // Gold
                "Sad" -> Color(0xFF1E90FF)   // Dodger Blue
                "Energetic" -> Color(0xFFFF4500) // Orange Red
                "Calm" -> Color(0xFF98FB98)  // Pale Green
                "Romantic" -> Color(0xFFFF69B4) // Hot Pink
                "Party" -> Color(0xFF8A2BE2) // Blue Violet
                else -> Color(0xFF808080)    // Gray
            }
        },
        animationSpec = tween(1000)
    )

    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val scanLineAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val screenHeight = maxHeight

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraExecutor = Executors.newSingleThreadExecutor()
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, FaceAnalyzer { mood, detected ->
                                faceDetected = detected
                                if (!manualModeEnabled) {
                                    detectedMood = mood
                                }
                            })
                        }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        // Log error
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Radar Scanline Effect
        if (!manualModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .offset(y = (screenHeight.value * scanLineAnim - 150).dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, glowColor.copy(alpha = 0.4f), glowColor.copy(alpha = 0.8f))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = (screenHeight.value * scanLineAnim).dp)
                    .shadow(16.dp, spotColor = glowColor)
                    .background(glowColor)
            )
        }

        // Center Face Frame / Scanning Ring
        if (!manualModeEnabled) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(
                            colors = if (faceDetected) {
                                listOf(Color(0xFF00FFCC), Color(0xFF00FF66), Color(0xFF00FFCC))
                            } else {
                                listOf(Color(0xFFFF3366), Color(0xFFFF9900), Color(0xFFFF3366))
                            }
                        ),
                        shape = CircleShape
                    )
                    .padding(8.dp)
            ) {
                // Inner subtle scanning circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.2.dp,
                            color = (if (faceDetected) Color(0xFF00FFCC) else Color(0xFFFF3366)).copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }

        // Overlay UI
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Top Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Top Indicator
                Box(
                    modifier = Modifier
                        .padding(top = 48.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(2.dp, glowColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (if (manualModeEnabled) "MANUAL: " else "") + detectedMood.uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        if (!manualModeEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (faceDetected) "FACE DETECTED!" else "SCANNING FOR FACE...",
                                color = if (faceDetected) Color(0xFF00FF66) else Color(0xFFFF3366),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language Selectors
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    listOf("English", "Hindi", "Punjabi").forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang },
                            label = { Text(lang) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Manual Mode Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Manual Mood", color = Color.White)
                    Switch(
                        checked = manualModeEnabled,
                        onCheckedChange = { manualModeEnabled = it },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                if (manualModeEnabled) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        items(moodsList.size) { index ->
                            val mood = moodsList[index]
                            FilterChip(
                                selected = detectedMood == mood,
                                onClick = { detectedMood = mood },
                                label = { Text(mood) }
                            )
                        }
                    }
                }
            }

            // Bottom Floating Action Button
            Button(
                onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onMoodCaptured(detectedMood, selectedLanguage) 
                },
                modifier = Modifier
                    .padding(bottom = 64.dp)
                    .height(72.dp)
                    .width(280.dp)
                    .shadow(24.dp, RoundedCornerShape(36.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = glowColor),
                shape = RoundedCornerShape(36.dp)
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = "Music", modifier = Modifier.size(28.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "FIND MUSIC",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
