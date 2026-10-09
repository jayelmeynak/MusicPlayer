package com.jayelmeynak.feature.player.ui

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.designsystem.components.SeekSlider
import com.jayelmeynak.lib.navigation.Navigator

/**
 * The current track over the tab content; draws nothing until the playback session is connected
 * and while the queue is empty. A click opens [PlayerDestination] through [navigator].
 *
 * Call it outside the navigation entries: its view model then lives as long as the Activity.
 */
@Composable
public fun MiniPlayer(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    MiniPlayer(viewModel = hiltViewModel(), navigator = navigator, modifier = modifier)
}

@Composable
internal fun MiniPlayer(
    viewModel: MiniPlayerViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.visible) return
    MiniPlayerContent(
        state = state,
        onAction = viewModel::onAction,
        onClick = { navigator.navigateTo(PlayerDestination) },
        modifier = modifier,
    )
}

@Composable
private fun MiniPlayerContent(
    state: MiniPlayerUiState,
    onAction: (MiniPlayerAction) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = state.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onAction(MiniPlayerAction.TogglePlayPause) }) {
                Icon(
                    imageVector = if (state.showPlayButton) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (state.showPlayButton) "Воспроизведение" else "Пауза",
                )
            }
        }
        SeekSlider(
            progress = state.progress,
            onSeek = { percent -> onAction(MiniPlayerAction.SeekTo(percent)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .padding(8.dp),
            trackKey = state.trackKey,
        )
    }
}
