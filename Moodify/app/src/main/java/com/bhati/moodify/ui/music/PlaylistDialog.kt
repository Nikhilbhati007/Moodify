package com.bhati.moodify.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhati.moodify.data.model.TrackDto

@Composable
fun PlaylistSelectionDialog(
    track: TrackDto,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select a playlist for \"${track.title}\"", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                if (playlists.isEmpty()) {
                    Text("No playlists found. Create one first!")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                modifier = Modifier.clickable {
                                    viewModel.addTrackToPlaylist(playlist.id, track)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
