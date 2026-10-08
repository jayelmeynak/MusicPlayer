package com.jayelmeynak.feature.player.ui

internal sealed interface MiniPlayerAction {
    data object TogglePlayPause : MiniPlayerAction

    /** The seek gesture ended at [percent] of the duration. */
    data class SeekTo(val percent: Float) : MiniPlayerAction
}
