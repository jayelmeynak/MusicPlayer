package com.jayelmeynak.musicplayer.presentation.navigation

sealed class Screen(
    val route: String,
    val name: String
) {
    object ApiTracks : Screen(ROUTE_API_TRACKS, "Remote")
    object DownloadTracks : Screen(ROUTE_DOWNLOADED_TRACKS, "Local")

    companion object {
        const val ROUTE_API_TRACKS = "apiTracks"
        const val ROUTE_DOWNLOADED_TRACKS = "downloadedTracks"
    }
}