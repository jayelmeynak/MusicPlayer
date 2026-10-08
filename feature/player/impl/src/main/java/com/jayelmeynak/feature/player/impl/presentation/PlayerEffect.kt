package com.jayelmeynak.feature.player.impl.presentation

internal sealed interface PlayerEffect {
    /** Nothing to play (e.g. the screen was restored after process death): leave the screen. */
    data object Close : PlayerEffect
}
