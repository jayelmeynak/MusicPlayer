package com.jayelmeynak.feature.player.api

import androidx.compose.runtime.Composable
import com.jayelmeynak.lib.navigation.Navigator

/** Lets the host shell show the mini player without depending on the player implementation. */
public interface MiniPlayerHost {

    /**
     * The mini player for the current track; draws nothing while the queue is empty.
     * A click opens [PlayerDestination] through [navigator].
     */
    @Composable
    public fun MiniPlayer(navigator: Navigator)
}
