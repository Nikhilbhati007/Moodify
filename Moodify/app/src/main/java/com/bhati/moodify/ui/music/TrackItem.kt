package com.bhati.moodify.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bhati.moodify.data.model.TrackDto

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrackItem(
    track: TrackDto,
    isLiked: Boolean,
    isPlaying: Boolean,
    showPause: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color.LightGray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (isPlaying) {
             Box(
                 modifier = Modifier
                     .size(32.dp)
                     .clip(CircleShape)
                     .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                 contentAlignment = Alignment.Center
             ) {
                 Icon(
                     imageVector = if (showPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                     contentDescription = "Playing",
                     tint = MaterialTheme.colorScheme.primary,
                     modifier = Modifier.size(20.dp)
                 )
             }
             Spacer(modifier = Modifier.width(8.dp))
        }
        
        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.LightGray
            )
        }
    }
}
