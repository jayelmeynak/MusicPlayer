package com.jayelmeynak.musicplayer.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jayelmeynak.download_tracks.presentation.DownloadTrackScreen
import com.jayelmeynak.search_tracks.presentation.ChartTracksScreen

@Composable
fun Navigation(
    scaffoldPadding: PaddingValues,
    navController: NavHostController,
    onTrackClicked: (Uri) -> Unit
) {

    NavHost(
        navController = navController,
        startDestination = Screen.ROUTE_API_TRACKS
    ) {
        composable(Screen.ROUTE_API_TRACKS) {
            ChartTracksScreen(
                scaffoldPadding = scaffoldPadding
            ) { trackId ->
                val source = "api"
                val deepLinkUri =
                    Uri.parse("multiplayer://player?source=${source}&trackId=${trackId}")
                onTrackClicked(deepLinkUri)
            }
        }
        composable(Screen.ROUTE_DOWNLOADED_TRACKS) {
            DownloadTrackScreen(
                scaffoldPadding = scaffoldPadding
            ) { trackUri ->
                val source = "local"
                val deepLinkUri =
                    Uri.parse("multiplayer://player?source=${source}&trackUri=${Uri.encode(trackUri.toString())}")
                onTrackClicked(deepLinkUri)
            }
        }
    }
}