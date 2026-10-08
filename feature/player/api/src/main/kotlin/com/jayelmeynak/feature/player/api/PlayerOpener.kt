package com.jayelmeynak.feature.player.api

/**
 * Asks the player to play a track the next time [PlayerDestination] is opened.
 *
 * Call [open] before navigating to [PlayerDestination]; the latest request wins.
 */
public interface PlayerOpener {
    public fun open(source: TrackSource, id: String)
}
