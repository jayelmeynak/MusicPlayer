package com.jayelmeynak.feature.player.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The player screen: shows the current item of [PlaybackController]. To play a list, call
 * [PlaybackController.play] and then navigate here.
 */
@Serializable
public data object PlayerDestination : NavKey
