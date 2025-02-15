package com.jayelmeynak.player.presentation


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.jayelmeynak.player.R

@Composable
fun PlayerScreen(
    viewModel: AudioViewModel = hiltViewModel()
) {

    val currentTrack = viewModel.currentSelectedAudio
    val state = viewModel.uiState.collectAsState()

    when (state.value) {
        is UIState.Initial -> {

        }

        is UIState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is UIState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = (state.value as UIState.Error).errorMessage.asString())
            }
        }

        is UIState.Ready -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = currentTrack.album?.cover ?: R.drawable.track_place_holder
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = currentTrack.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentTrack.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Slider(
                        value = viewModel.progress,
                        onValueChange = {
                            viewModel.onUiEvents(UIEvents.SeekTo(it))
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.progressString,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Text(
                            text = viewModel.formatDuration(viewModel.duration),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.onUiEvents(UIEvents.Backward)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastRewind,
                            contentDescription = "Пролистать назад"
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.onUiEvents(UIEvents.SeekToPrevious)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Предыдущий трек"
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.onUiEvents(UIEvents.PlayPause)
                        }
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (viewModel.isPlaying) "Пауза" else "Воспроизведение"
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.onUiEvents(UIEvents.SeekToNext)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Следующий трек"
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.onUiEvents(UIEvents.Forward)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = "Пролистать вперед"
                        )
                    }
                }
            }
        }
    }
}
