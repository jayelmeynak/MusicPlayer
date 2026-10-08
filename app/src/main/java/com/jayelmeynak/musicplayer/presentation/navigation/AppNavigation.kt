package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.jayelmeynak.download_tracks.presentation.DownloadTrackScreen
import com.jayelmeynak.feature.player.api.MiniPlayerHost
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.search_tracks.presentation.ChartTracksScreen

@Composable
fun AppNavigation(
    entryInstallers: Set<EntryInstaller>,
    playbackController: PlaybackController,
    miniPlayerHost: MiniPlayerHost,
) {
    val navigationState = rememberNavigationState(
        startRoute = TopLevelDestination.ApiTracks,
        topLevelRoutes = setOf(TopLevelDestination.ApiTracks, TopLevelDestination.DownloadTracks),
    )
    val navigator = remember(navigationState) { AppNavigator(navigationState) }
    val currentTop = navigationState.currentTopLevel
    val currentBackStack = navigationState.backStacks[currentTop] ?: emptyList()
    val currentRoute = currentBackStack.lastOrNull()

    val isPlayerScreen = currentRoute is PlayerDestination

    val entries = navigationState.toEntries(
        entryProvider {
            entry<TopLevelDestination.ApiTracks> {
                ChartTracksScreen(
                    scaffoldPadding = PaddingValues(),
                    onTrackClicked = { tracks, index ->
                        playbackController.play(tracks.map { it.toQueueItem() }, index)
                        navigator.navigateTo(PlayerDestination)
                    },
                )
            }
            entry<TopLevelDestination.DownloadTracks> {
                DownloadTrackScreen(
                    scaffoldPadding = PaddingValues(),
                    viewModel = hiltViewModel(),
                    onTrackClicked = { tracks, index ->
                        playbackController.play(tracks.map { it.toQueueItem() }, index)
                        navigator.navigateTo(PlayerDestination)
                    },
                )
            }
            entryInstallers.forEach { installer -> with(installer) { install(navigator) } }
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

            if (!isPlayerScreen) {
                miniPlayerHost.MiniPlayer(navigator)
            }
        }
    }
}
