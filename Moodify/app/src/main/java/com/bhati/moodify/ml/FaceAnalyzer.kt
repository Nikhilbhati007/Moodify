package com.bhati.moodify.ml

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val onMoodResult: (mood: String, faceDetected: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Throttle frames to every 400ms for smooth real-time response
    private var lastAnalyzedTimestamp = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp >= 400) {
            
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            val face = faces.first()
                            
                            val smileProb = face.smilingProbability ?: 0f
                            val leftEye = face.leftEyeOpenProbability ?: 0f
                            val rightEye = face.rightEyeOpenProbability ?: 0f
                            val avgEyeOpen = (leftEye + rightEye) / 2f
                            
                            // Get head Euler angles for tilt and turn
                            val tiltZ = Math.abs(face.headEulerAngleZ)
                            val turnY = Math.abs(face.headEulerAngleY)

                            // Calibrated threshold tree matching all 7 app moods
                            val mood = when {
                                smileProb > 0.65f && (tiltZ > 8f || turnY > 8f) -> "Party"      // Playful tilt/turn with smile
                                smileProb > 0.65f -> "Energetic"                              // Wide smile
                                smileProb > 0.35f -> "Happy"                                   // Normal smile
                                smileProb in 0.15f..0.35f && avgEyeOpen < 0.82f -> "Romantic"  // Dreamy subtle smile with half-closed eyes
                                smileProb > 0.12f -> "Calm"                                    // Relaxed mild smile
                                smileProb < 0.1f && avgEyeOpen < 0.78f -> "Sad"                // Droopy expression
                                else -> "Neutral"                                              // Default neutral
                            }
                            
                            // Post safely to Compose state on the main thread
                            mainHandler.post {
                                onMoodResult(mood, true)
                            }
                        } else {
                            // No face detected in this frame
                            mainHandler.post {
                                onMoodResult("Neutral", false)
                            }
                        }
                    }
                    .addOnFailureListener { exc ->
                        Log.e("FaceAnalyzer", "ML Kit face detection failed", exc)
                        mainHandler.post {
                            onMoodResult("Neutral", false)
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
            lastAnalyzedTimestamp = currentTimestamp
        } else {
            imageProxy.close()
        }
    }
}
