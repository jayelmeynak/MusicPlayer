package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Immutable
sealed interface TopLevelDestination : NavKey {
    @Serializable
    data object ApiTracks : TopLevelDestination
    @Serializable
    data object DownloadTracks : TopLevelDestination
}
