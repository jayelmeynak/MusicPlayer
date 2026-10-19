package com.jayelmeynak.musicplayer.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation3.runtime.NavKey
import com.jayelmeynak.feature.player.api.OpenPlayerRequest
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.lib.navigation.EntryInstaller
import com.jayelmeynak.musicplayer.presentation.navigation.AppNavigation
import com.jayelmeynak.lib.designsystem.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

/**
 * Единственная Activity (`singleTop`): тема и навигация. Разрешений не запрашивает — их просят
 * экраны, которым они нужны (вкладка «Local» — доступ к аудио, экран плеера — уведомления).
 *
 * Интент с [OpenPlayerRequest.ACTION] (тап по уведомлению плеера) — запрос оболочке открыть плеер:
 * в новом запуске и в [onNewIntent]. Пересоздание (поворот, смерть процесса) и запуск из «Недавних»
 * запрос не повторяют: стеки уже восстановлены, а «Недавние» воспроизводят исходный интент задачи.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var entryInstallers: Set<@JvmSuppressWildcards EntryInstaller>

    @Inject
    lateinit var playbackController: PlaybackController

    /** Ключи, которые оболочка открывает по внешним интентам; запрос из onCreate ждёт композиции. */
    private val externalDestinationRequests = Channel<NavKey>(Channel.CONFLATED)
    private val externalDestinations = externalDestinationRequests.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleExternalIntent(intent)
        setContent {
            AppTheme {
                AppNavigation(
                    entryInstallers = entryInstallers,
                    playbackController = playbackController,
                    externalDestinations = externalDestinations,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExternalIntent(intent)
    }

    /** Смотрит только действие и флаг истории; extras не читаются. */
    private fun handleExternalIntent(intent: Intent?) {
        if (intent?.action != OpenPlayerRequest.ACTION) return
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        externalDestinationRequests.trySend(OpenPlayerRequest.destination)
    }
}
