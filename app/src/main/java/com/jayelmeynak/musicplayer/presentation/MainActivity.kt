package com.jayelmeynak.musicplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.musicplayer.presentation.navigation.AppNavigation
import com.jayelmeynak.lib.designsystem.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Единственная Activity: тема и навигация. Разрешений не запрашивает — их просят экраны, которым
 * они нужны (вкладка «Local» — доступ к аудио, экран плеера — уведомления).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var entryInstallers: Set<@JvmSuppressWildcards EntryInstaller>

    @Inject
    lateinit var playbackController: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppNavigation(
                    entryInstallers = entryInstallers,
                    playbackController = playbackController,
                )
            }
        }
    }
}
