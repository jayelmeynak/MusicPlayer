package com.jayelmeynak.feature.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
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
        if (duration <= 0) return
        playbackController.seekTo((duration * percent / PERCENT).toLong())
    }

    private fun toUiState(playback: PlaybackState, positionMs: Long): MiniPlayerUiState {
        val active = playback as? PlaybackState.Active ?: return MiniPlayerUiState()
        // An item added by another controller has no metadata of ours: nothing to show.
        val current = active.current ?: return MiniPlayerUiState()
        val duration = active.durationMs
        return MiniPlayerUiState(
            title = current.title,
            artist = current.artist,
            showPlayButton = active.showPlayButton,
            progress = if (duration > 0) (positionMs.toFloat() / duration * PERCENT).coerceIn(0f, PERCENT) else 0f,
            trackKey = "${current.source}|${current.id}",
            visible = true,
        )
    }

    private companion object {
        const val PERCENT = 100f

        /** Keeps the upstream through a configuration change. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
