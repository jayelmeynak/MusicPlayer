package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.ui.NavDisplay
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
    val navigationState = rememberNavigationState(startTab = AppTab.Catalog, tabs = AppTab.entries)
    val navigator = remember(navigationState) { AppNavigator(navigationState) }
    val currentTab = navigationState.currentTab
    val currentBackStack = navigationState.backStacks[currentTab] ?: emptyList()
    val currentRoute = currentBackStack.lastOrNull()

    val isPlayerScreen = currentRoute is PlayerDestination

    val entryProvider = remember(entryInstallers, navigator) { appEntryProvider(entryInstallers, navigator) }
    val entries = navigationState.toEntries(entryProvider)

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppTab.entries.forEach { tab ->
                val selected = currentTab == tab
                item(
                    icon = { Icon(imageVector = tab.icon(selected), contentDescription = stringResource(tab.label)) },
                    label = { Text(stringResource(tab.label)) },
                    selected = selected,
                    onClick = { navigator.navigateToRoot(tab) },
                )
            }
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
