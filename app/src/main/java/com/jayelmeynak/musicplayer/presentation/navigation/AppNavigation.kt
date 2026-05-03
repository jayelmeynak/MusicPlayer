package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.jayelmeynak.download_tracks.presentation.DownloadTrackScreen
import com.jayelmeynak.player.presentation.AudioViewModel
import com.jayelmeynak.player.presentation.PlayerScreen
import com.jayelmeynak.player.presentation.UIEvents
import com.jayelmeynak.search_tracks.presentation.ChartTracksScreen

@Composable
fun AppNavigation(
    startService: () -> Unit,
) {
    val activity = LocalActivity.current
    val audioViewModel: AudioViewModel = hiltViewModel(activity as ViewModelStoreOwner)
    val navigationState = rememberNavigationState(
        startRoute = TopLevelDestination.ApiTracks,
        topLevelRoutes = setOf(TopLevelDestination.ApiTracks, TopLevelDestination.DownloadTracks),
    )
    val navigator = remember(navigationState) { AppNavigator(navigationState) }
    val currentTop = navigationState.currentTopLevel
    val currentBackStack = navigationState.backStacks[currentTop] ?: emptyList()
    val currentRoute = currentBackStack.lastOrNull()

    val isPlayerScreen = currentRoute is AppDestination.PlayerApi ||
            currentRoute is AppDestination.PlayerLocal

    val audioList by audioViewModel.audioList.collectAsStateWithLifecycle()
    val isPlayerVisible = audioList.isNotEmpty() && !isPlayerScreen

    val entries = navigationState.toEntries(
        entryProvider {
            entry<TopLevelDestination.ApiTracks> {
                ChartTracksScreen(
                    scaffoldPadding = PaddingValues(),
                    onTrackClicked = { trackId ->
                        navigator.navigateTo(AppDestination.PlayerApi(trackId))
                    },
                )
            }
            entry<TopLevelDestination.DownloadTracks> {
                DownloadTrackScreen(
                    scaffoldPadding = PaddingValues(),
                    viewModel = hiltViewModel(),
                    onTrackClicked = { trackUri ->
                        navigator.navigateTo(AppDestination.PlayerLocal(trackUri.toString()))
                    },
                )
            }
            entry<AppDestination.PlayerApi> { key ->
                LaunchedEffect(Unit) { startService() }
                PlayerScreen(
                    viewModel = audioViewModel,
                    scaffoldPadding = PaddingValues(),
                    source = "api",
                    idOrUri = key.trackId,
                )
            }
            entry<AppDestination.PlayerLocal> { key ->
                LaunchedEffect(Unit) { startService() }
                PlayerScreen(
                    viewModel = audioViewModel,
                    scaffoldPadding = PaddingValues(),
                    source = "local",
                    idOrUri = key.trackUri,
                )
            }
        }
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                icon = {
                    Icon(
                        imageVector = if (currentTop == TopLevelDestination.ApiTracks) Icons.Filled.Wifi else Icons.Outlined.Wifi,
                        contentDescription = "Remote",
                    )
                },
                label = { Text("Remote") },
                selected = currentTop == TopLevelDestination.ApiTracks,
                onClick = { navigator.navigateToRoot(TopLevelDestination.ApiTracks) },
            )
            item(
                icon = {
                    Icon(
                        imageVector = if (currentTop == TopLevelDestination.DownloadTracks) Icons.Filled.SdStorage else Icons.Outlined.SdStorage,
                        contentDescription = "Local",
                    )
                },
                label = { Text("Local") },
                selected = currentTop == TopLevelDestination.DownloadTracks,
                onClick = { navigator.navigateToRoot(TopLevelDestination.DownloadTracks) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                NavDisplay(
                    entries = entries,
                    onBack = { navigator.goBack() },
                )
            }

            if (isPlayerVisible) {
                MiniPlayer(
                    viewModel = audioViewModel,
                    onPlayerClick = { source, id ->
                        if (source == "local") {
                            navigator.navigateTo(AppDestination.PlayerLocal(id))
                        } else {
                            navigator.navigateTo(AppDestination.PlayerApi(id))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    viewModel: AudioViewModel,
    onPlayerClick: (String, String) -> Unit,
) {
    val currentTrack by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .clickable {
                if (source == "api") {
                    onPlayerClick("api", currentTrack.id.toString())
                } else {
                    onPlayerClick("local", currentTrack.preview)
                }
            },
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
                    text = currentTrack.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { viewModel.onUiEvents(UIEvents.PlayPause) }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                )
            }
        }
        Slider(
            value = progress,
            onValueChange = { viewModel.onUiEvents(UIEvents.SeekTo(it)) },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .padding(8.dp),
        )
    }
}
