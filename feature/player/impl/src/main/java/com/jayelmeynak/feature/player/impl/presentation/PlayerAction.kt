package com.jayelmeynak.feature.player.impl.presentation

internal sealed interface PlayerAction {
    data object TogglePlayPause : PlayerAction
    data object Next : PlayerAction
    data object Previous : PlayerAction
    data object SeekBack : PlayerAction
    data object SeekForward : PlayerAction

    /** The seek gesture ended at [percent] of the duration. */
    data class SeekTo(val percent: Float) : PlayerAction
}
