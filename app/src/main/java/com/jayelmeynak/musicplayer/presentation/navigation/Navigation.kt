package com.jayelmeynak.musicplayer.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jayelmeynak.download_tracks.presentation.DownloadTrackScreen
import com.jayelmeynak.player.presentation.AudioViewModel
import com.jayelmeynak.player.presentation.PlayerScreen
import com.jayelmeynak.search_tracks.presentation.ChartTracksScreen

@Composable
fun Navigation(
    viewModel: AudioViewModel,
    scaffoldPadding: PaddingValues,
    navController: NavHostController,
    startService: () -> Unit
) {

    NavHost(
        navController = navController,
        startDestination = Screen.ROUTE_API_TRACKS
    ) {
        composable(Screen.ROUTE_API_TRACKS) {
            ChartTracksScreen(
                scaffoldPadding = scaffoldPadding
            ) { trackId ->
                navController.navigate(Screen.ROUTE_PLAYER + "/api/${trackId}")
            }
        }
        composable(Screen.ROUTE_DOWNLOADED_TRACKS) {
            DownloadTrackScreen(
                scaffoldPadding = scaffoldPadding
            ) { trackUri ->
                navController.navigate(Screen.ROUTE_PLAYER + "/local/${Uri.encode(trackUri.toString())}")
            }
        }
        composable(
            route = Screen.ROUTE_PLAYER + "/api/{trackId}",
            arguments = listOf(
                navArgument("trackId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            startService()
            PlayerScreen(
                viewModel = viewModel,
                scaffoldPadding = scaffoldPadding,
                source = "api",
                idOrUri = backStackEntry.arguments?.getString("trackId") ?: ""
            )
        }
        composable(
            route = Screen.ROUTE_PLAYER + "/local/{trackUri}",
            arguments = listOf(
                navArgument("trackUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            startService()
            PlayerScreen(
                viewModel = viewModel,
                scaffoldPadding = scaffoldPadding,
                source = "local",
                idOrUri = backStackEntry.arguments?.getString("trackUri") ?: ""
            )
        }
    }
}