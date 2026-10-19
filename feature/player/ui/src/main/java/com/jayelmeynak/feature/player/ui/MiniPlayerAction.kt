package com.jayelmeynak.feature.player.ui

internal sealed interface MiniPlayerAction {
    data object TogglePlayPause : MiniPlayerAction

    /** Жест перемотки закончился на [percent] длительности. */
    data class SeekTo(val percent: Float) : MiniPlayerAction
}
