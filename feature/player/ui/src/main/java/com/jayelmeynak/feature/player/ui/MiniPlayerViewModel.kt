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
 * The mini player over [PlaybackController]: the current item, its progress and play/pause.
 * Scoped to the host outside the navigation entries, so it lives as long as the Activity.
 */
@HiltViewModel
internal class MiniPlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    /**
     * Collects the controller only while the mini player is shown: the view model lives as long
     * as the Activity, and the position ticks every half second during playback.
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

    private fun seekTo(percent: Float) {
        val duration = (playbackController.state.value as? PlaybackState.Active)?.durationMs ?: return
        positionForPercent(percent, duration)?.let(playbackController::seekTo)
    }

    private fun toUiState(playback: PlaybackState, positionMs: Long): MiniPlayerUiState {
        // Снапшот без подключения тоже показывается: мини-плеер не мигает при возврате из фона.
        // Idle и ConnectionFailed — показывать нечего.
        val active = playback as? PlaybackState.Active ?: return MiniPlayerUiState()
        // An item added by another controller has no metadata of ours: nothing to show.
        val current = active.current ?: return MiniPlayerUiState()
        return MiniPlayerUiState(
            title = current.title,
            artist = current.artist,
            showPlayButton = active.showPlayButton,
            progress = progressPercent(positionMs, active.durationMs),
            trackKey = current.key,
            visible = true,
        )
    }

    private companion object {
        /** Keeps the upstream through a configuration change. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
