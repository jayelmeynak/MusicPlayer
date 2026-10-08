package com.jayelmeynak.feature.player.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The player screen. Shows the track requested through [PlayerOpener] or, without a new request,
 * the current one.
 */
@Serializable
public data object PlayerDestination : NavKey
