package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.impl.presentation.components.SeekSlider
import com.jayelmeynak.lib.navigation.Navigator

/**
 * The current track over the tab content; draws nothing until the playback session is connected
 * and while the queue is empty.
 *
 * Reads [PlaybackController] directly until the mini player gets its own view model.
 */
@Composable
internal fun MiniPlayer(
    playbackController: PlaybackController,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val playback by playbackController.state.collectAsStateWithLifecycle()
    val active = playback as? PlaybackState.Active ?: return
    val currentTrack = active.current ?: return
    val positionMs by playbackController.positionMs.collectAsStateWithLifecycle()
    val durationMs = active.durationMs
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs * 100f).coerceIn(0f, 100f) else 0f

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .clickable { navigator.navigateTo(PlayerDestination) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentTrack.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = playbackController::togglePlayPause) {
                Icon(
                    imageVector = if (active.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (active.isPlaying) "Пауза" else "Воспроизведение",
                )
            }
        }
        SeekSlider(
            progress = progress,
            onSeek = { percent ->
                if (durationMs > 0) playbackController.seekTo((durationMs * percent / 100f).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .padding(8.dp),
            trackKey = "${currentTrack.source}|${currentTrack.id}",
        )
    }
}
