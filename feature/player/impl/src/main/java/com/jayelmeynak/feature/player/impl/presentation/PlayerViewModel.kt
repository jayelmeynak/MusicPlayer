package com.jayelmeynak.feature.player.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackError
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.impl.R
import com.jayelmeynak.feature.player.impl.artwork.LocalArtworkSource
import com.jayelmeynak.lib.designsystem.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The player screen over [PlaybackController]: shows its current item and sends the transport
 * commands. Scoped to the screen's NavEntry; the playback state itself lives in the session, so a
 * new instance shows the current track right away.
 */
@HiltViewModel
internal class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val artworkSource: LocalArtworkSource,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Connecting)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _effects = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerEffect> = _effects.receiveAsFlow()

    private var closeSent = false
    private var artworkJob: Job? = null
    private var artworkFor: QueueItem? = null
    private var artworkData: ByteArray? = null

    init {
        viewModelScope.launch {
            playbackController.state.collect(::onPlaybackState)
        }
        viewModelScope.launch {
            playbackController.positionMs.collect { position ->
                _state.update { state ->
                    if (state is PlayerUiState.Ready) state.copy(positionMs = position) else state
                }
            }
        }
    }

    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.TogglePlayPause -> playbackController.togglePlayPause()
            PlayerAction.Next -> playbackController.next()
            PlayerAction.Previous -> playbackController.previous()
            PlayerAction.SeekBack -> playbackController.seekBack()
            PlayerAction.SeekForward -> playbackController.seekForward()
            is PlayerAction.SeekTo -> seekTo(action.percent)
        }
    }

    private fun seekTo(percent: Float) {
        val duration = (_state.value as? PlayerUiState.Ready)?.durationMs ?: return
        if (duration <= 0) return
        playbackController.seekTo((duration * percent / PERCENT).toLong())
    }

    private fun onPlaybackState(playback: PlaybackState) {
        val active = playback as? PlaybackState.Active
        if (active == null) {
            _state.value = PlayerUiState.Connecting
            return
        }
        if (active.queue.isEmpty()) {
            _state.value = PlayerUiState.Connecting
            if (!closeSent) {
                closeSent = true
                _effects.trySend(PlayerEffect.Close)
            }
            return
        }
        // A foreign current item (added by another controller) has no metadata of ours: show the
        // controls without track details rather than an endless progress indicator.
        val current = active.current
        if (current != artworkFor) loadArtwork(current)
        _state.value = PlayerUiState.Ready(
            trackKey = current?.let { "${it.source}|${it.id}" } ?: "foreign|${active.currentIndex}",
            title = current?.title.orEmpty(),
            artist = current?.artist.orEmpty(),
            artworkUri = current?.artworkUri?.takeIf { current.source == TrackSource.DEEZER },
            artworkData = artworkData,
            isPreview = current?.source == TrackSource.DEEZER,
            trackLengthMs = current?.durationMs ?: 0L,
            showPlayButton = active.showPlayButton,
            isBuffering = active.isBuffering,
            durationMs = active.durationMs,
            positionMs = playbackController.positionMs.value,
            errorMessage = active.error?.toUiText(),
        )
    }

    /** A new current item drops the previous cover and any load still running for it. */
    private fun loadArtwork(item: QueueItem?) {
        artworkJob?.cancel()
        artworkFor = item
        artworkData = null
        if (item?.source != TrackSource.LOCAL) return
        artworkJob = viewModelScope.launch {
            val data = artworkSource.artwork(item.id) ?: return@launch
            artworkData = data
            _state.update { state ->
                if (state is PlayerUiState.Ready) state.copy(artworkData = data) else state
            }
        }
    }

    private fun PlaybackError.toUiText(): UiText = UiText.StringResourceId(
        when (this) {
            PlaybackError.SOURCE_UNAVAILABLE -> R.string.player_error_source_unavailable
            PlaybackError.UNKNOWN -> R.string.player_error_unknown
        }
    )

    private companion object {
        const val PERCENT = 100f
    }
}
