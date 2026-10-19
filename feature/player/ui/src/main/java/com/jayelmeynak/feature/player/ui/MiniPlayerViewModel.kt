package com.jayelmeynak.feature.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.feature.player.api.positionForPercent
import com.jayelmeynak.feature.player.api.progressPercent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Мини-плеер поверх [PlaybackController]: текущий элемент, его прогресс и play/pause.
 * Скоуп — хост вне записей навигации, поэтому живёт столько же, сколько Activity.
 */
@HiltViewModel
internal class MiniPlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    /**
     * Собирает контроллер, только пока мини-плеер показан: view model живёт столько же, сколько
     * Activity, а позиция во время воспроизведения тикает каждые полсекунды.
     */
    val state: StateFlow<MiniPlayerUiState> =
        combine(playbackController.state, playbackController.positionMs, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MiniPlayerUiState())

    fun onAction(action: MiniPlayerAction) {
        when (action) {
            MiniPlayerAction.TogglePlayPause -> playbackController.togglePlayPause()
            is MiniPlayerAction.SeekTo -> seekTo(action.percent)
        }
    }

    /** Перемотка недоступна — команда не отправляется: сессия её всё равно отбросит. */
    private fun seekTo(percent: Float) {
        val active = (playbackController.state.value as? PlaybackState.Active)?.takeIf { it.canSeek } ?: return
        positionForPercent(percent, active.durationMs)?.let(playbackController::seekTo)
    }

    private fun toUiState(playback: PlaybackState, positionMs: Long): MiniPlayerUiState {
        // Снапшот без подключения тоже показывается: мини-плеер не мигает при возврате из фона.
        // Idle и ConnectionFailed — показывать нечего.
        val active = playback as? PlaybackState.Active ?: return MiniPlayerUiState()
        // У элемента, добавленного другим контроллером, нет наших метаданных: показывать нечего.
        val current = active.current ?: return MiniPlayerUiState()
        return MiniPlayerUiState(
            title = current.title,
            artist = current.artist,
            showPlayButton = active.showPlayButton,
            progress = progressPercent(positionMs, active.durationMs),
            trackKey = current.key,
            // Ползунок перематывает по доле длительности: без неё жест ничего не отправит.
            canSeek = active.canSeek && active.durationMs > 0,
            visible = true,
        )
    }

    private companion object {
        /** Держит upstream через смену конфигурации. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
