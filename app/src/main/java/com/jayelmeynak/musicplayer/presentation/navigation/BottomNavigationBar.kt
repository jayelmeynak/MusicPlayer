package com.jayelmeynak.musicplayer.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val unselectedIcons =
        listOf(Icons.Outlined.Wifi, Icons.Outlined.SdStorage)
    val selectedIcons =
        listOf(Icons.Filled.Wifi, Icons.Filled.SdStorage)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var isNavigateNow = false
    val currentRoute =
        when (navBackStackEntry?.destination?.route) {
            Screen.ROUTE_API_TRACKS -> 0
            Screen.ROUTE_DOWNLOADED_TRACKS -> 1
            else -> 0
        }

    NavigationBar {
        NavBarItems.items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (currentRoute == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = item.name
                    )
                },
                label = { Text(item.name) },
                selected = currentRoute == index,
                onClick = {
                    if(!isNavigateNow && currentRoute != index) {
                        isNavigateNow = true
                        navController.navigate(item.route) {
                            popUpTo(item.route) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }
    }
}