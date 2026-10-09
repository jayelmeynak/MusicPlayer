package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.jayelmeynak.feature.player.impl.presentation.components.PlayPauseIconButton
import com.jayelmeynak.lib.designsystem.components.SeekSlider
import com.jayelmeynak.lib.designsystem.R
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.jayelmeynak.feature.player.impl.R as PlayerR

@Composable
internal fun PlayerScreen(
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        PlayerUiState.Connecting -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is PlayerUiState.Ready -> PlayerContent(state, onAction, modifier)
    }
}

@Composable
private fun PlayerContent(
    state: PlayerUiState.Ready,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = state.artworkUri ?: state.artworkData ?: R.drawable.track_place_holder
            ),
            contentDescription = null,
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )

        Column(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            if (state.isPreview) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (state.trackLengthMs > 0) {
                        stringResource(
                            PlayerR.string.player_preview_label_with_length,
                            formatDuration(state.trackLengthMs),
                        )
                    } else {
                        stringResource(PlayerR.string.player_preview_label)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SeekSlider(
                progress = state.progress,
                onSeek = { onAction(PlayerAction.SeekTo(it)) },
                modifier = Modifier.fillMaxWidth(),
                trackKey = state.trackKey,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = formatDuration(state.positionMs), style = MaterialTheme.typography.bodySmall)
                Text(text = formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(PlayerAction.SeekBack) }) {
                Icon(imageVector = Icons.Filled.FastRewind, contentDescription = "Пролистать назад")
            }
            IconButton(onClick = { onAction(PlayerAction.Previous) }) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Предыдущий трек")
            }
            PlayPauseIconButton(
                showPlayButton = state.showPlayButton,
                isBuffering = state.isBuffering,
                onIconButtonClick = { onAction(PlayerAction.TogglePlayPause) },
            )
            IconButton(onClick = { onAction(PlayerAction.Next) }) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Следующий трек")
            }
            IconButton(onClick = { onAction(PlayerAction.SeekForward) }) {
                Icon(imageVector = Icons.Filled.FastForward, contentDescription = "Пролистать вперед")
            }
        }
    }
}

/** `MM:SS` of [durationMs]. */
internal fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
