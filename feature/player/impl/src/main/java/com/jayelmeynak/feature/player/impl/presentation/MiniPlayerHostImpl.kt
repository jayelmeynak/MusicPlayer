package com.jayelmeynak.feature.player.impl.presentation

import androidx.compose.runtime.Composable
import com.jayelmeynak.feature.player.api.MiniPlayerHost
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.lib.navigation.Navigator
import javax.inject.Inject

/** Public only for the binding in `:di`; nothing outside the module can create it. */
public class MiniPlayerHostImpl @Inject internal constructor(
    private val playbackController: PlaybackController,
) : MiniPlayerHost {

    @Composable
    override fun MiniPlayer(navigator: Navigator) {
        MiniPlayer(playbackController = playbackController, navigator = navigator)
    }
}
