package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayelmeynak.lib.navigation.Navigator

/**
 * The player screen. Shows a progress indicator until the playback session is connected and
 * leaves itself once connected with nothing to play. It never starts the playback service: the
 * playback controller connects to it.
 */
@Composable
internal fun PlayerRoute(
    navigator: Navigator,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlayerEffect.Close -> navigator.goBack()
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(state = state, onAction = viewModel::onAction)
}
