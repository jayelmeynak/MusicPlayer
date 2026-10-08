package com.jayelmeynak.feature.player.impl.presentation

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import com.jayelmeynak.feature.player.impl.service.PlayBackService
import com.jayelmeynak.lib.navigation.Navigator

/**
 * The player screen: plays the requested track, if there is one, and starts the playback service.
 * With nothing to show (no request, empty queue) it closes itself instead of staying blank.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun PlayerRoute(navigator: Navigator) {
    PlayerRoute(viewModel = activityAudioViewModel(), navigator = navigator)
}

@OptIn(UnstableApi::class)
@Composable
internal fun PlayerRoute(viewModel: AudioViewModel, navigator: Navigator) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (!viewModel.onPlayerOpened()) {
            navigator.goBack()
            return@LaunchedEffect
        }
        context.startForegroundService(Intent(context, PlayBackService::class.java))
    }
    PlayerScreen(
        scaffoldPadding = PaddingValues(),
        viewModel = viewModel,
    )
}

/**
 * The single [AudioViewModel] of the Activity, shared by the player screen and the mini player.
 * A NavEntry-scoped instance would start with an empty state.
 */
@Composable
internal fun activityAudioViewModel(): AudioViewModel =
    hiltViewModel(checkNotNull(LocalActivity.current as? ViewModelStoreOwner) {
        "The player needs an Activity that owns view models"
    })
