package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jayelmeynak.player.presentation.AudioViewModel
import com.jayelmeynak.player.presentation.UIEvents

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    viewModel: AudioViewModel = hiltViewModel(),
) {
    val unselectedIcons =
        listOf(Icons.Outlined.Wifi, Icons.Outlined.SdStorage)
    val selectedIcons =
        listOf(Icons.Filled.Wifi, Icons.Filled.SdStorage)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var isNavigateNow = false
    val currentRoute =
        when (navBackStackEntry?.destination?.route) {
            Screen.ROUTE_API_TRACKS -> 0
            Screen.ROUTE_DOWNLOADED_TRACKS -> 1
            else -> 3
        }

    Column {
        if (viewModel.isPlaying && currentRoute != 3) {
            MiniPlayer()
        }

        NavigationBar {
            NavBarItems.items.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (currentRoute == index) selectedIcons[index] else unselectedIcons[index],
                            contentDescription = item.name
                        )
                    },
                    label = { Text(item.name) },
                    selected = currentRoute == index,
                    onClick = {
                        if (!isNavigateNow && currentRoute != index) {
                            isNavigateNow = true
                            navController.navigate(item.route) {
                                popUpTo(item.route) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    viewModel: AudioViewModel = hiltViewModel()
) {
//    var isPlaying = viewModel.isPlaying
//    var currentTrack = viewModel.currentSelectedAudio

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = viewModel.currentSelectedAudio.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = viewModel.currentSelectedAudio.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { viewModel.onUiEvents(UIEvents.PlayPause) }) {
            Icon(
                imageVector = if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (viewModel.isPlaying) "Пауза" else "Воспроизведение"
            )
        }
    }
}