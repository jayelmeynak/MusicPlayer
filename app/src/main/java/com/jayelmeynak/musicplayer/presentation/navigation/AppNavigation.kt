package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.jayelmeynak.feature.catalog.api.ChartDestination
import com.jayelmeynak.feature.localmusic.api.LocalMusicDestination
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlayerDestination
import com.jayelmeynak.feature.player.ui.MiniPlayer
import com.jayelmeynak.lib.navigation.EntryInstaller

/** Тег мини-плеера: заголовок трека есть и в списке, и в мини-плеере — UI-тесты ищут по тегу. */
const val MINI_PLAYER_TAG = "mini_player"

@Composable
fun AppNavigation(
    entryInstallers: Set<EntryInstaller>,
    playbackController: PlaybackController,
) {
    val navigationState = rememberNavigationState(
        startRoute = TopLevelDestination.ApiTracks,
        topLevelRoutes = setOf(TopLevelDestination.ApiTracks, TopLevelDestination.DownloadTracks),
        // Экраны вкладок регистрируют фичи: «Remote» — каталог (CatalogEntryInstaller),
        // «Local» — локальная музыка (LocalMusicEntryInstaller).
        roots = remember {
            mapOf(
                TopLevelDestination.ApiTracks to ChartDestination,
                TopLevelDestination.DownloadTracks to LocalMusicDestination,
            )
        },
    )
    val navigator = remember(navigationState) { AppNavigator(navigationState) }
    val currentTop = navigationState.topLevelRoute
    val currentBackStack = navigationState.backStacks[currentTop] ?: emptyList()
    val currentRoute = currentBackStack.lastOrNull()

    val isPlayerScreen = currentRoute is PlayerDestination

    val entries = navigationState.toEntries(
        entryProvider {
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

            // Outside NavDisplay: the mini player's view model is scoped to the Activity.
            if (!isPlayerScreen) {
                MiniPlayer(navigator = navigator, modifier = Modifier.testTag(MINI_PLAYER_TAG))
            }
        }
    }
}
