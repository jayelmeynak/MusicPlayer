package com.jayelmeynak.feature.player.impl.presentation

import android.annotation.SuppressLint
import android.content.ContentResolver
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.util.result.onError
import com.jayelmeynak.util.result.onSuccess
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.impl.domain.models.Album
import com.jayelmeynak.feature.player.impl.domain.models.Track
import com.jayelmeynak.feature.player.impl.domain.usecase.GetLocalTrackListUseCase
import com.jayelmeynak.feature.player.impl.domain.usecase.GetRemoteAlbumUseCase
import com.jayelmeynak.feature.player.impl.domain.usecase.GetRemoteTrackUseCase
import com.jayelmeynak.feature.player.impl.navigation.PlayerRequestHolder
import com.jayelmeynak.feature.player.impl.service.EXTRA_TRACK_DURATION_MS
import com.jayelmeynak.feature.player.impl.service.MusicServiceHandler
import com.jayelmeynak.feature.player.impl.service.MusicState
import com.jayelmeynak.feature.player.impl.service.PlayerEvent
import com.jayelmeynak.lib.designsystem.UiText
import com.jayelmeynak.lib.designsystem.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val audioDummy = Track(
    id = 0,
    title = "Title",
    artistName = "Artist",
    preview = "Preview",
    album = Album(1, "Album", "Album", "Album", "Album"),
    uri = null
)


@HiltViewModel
internal class AudioViewModel @Inject constructor(
    private val audioServiceHandler: MusicServiceHandler,
    private val getLocalTrackListUseCase: GetLocalTrackListUseCase,
    private val getRemoteTrackUseCase: GetRemoteTrackUseCase,
    private val getRemoteAlbumUseCase: GetRemoteAlbumUseCase,
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
    private val playerRequests: PlayerRequestHolder,
) : ViewModel() {

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _progressString = MutableStateFlow("00:00")
    val progressString: StateFlow<String> = _progressString.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSelectedAudio = MutableStateFlow(audioDummy)
    val currentSelectedAudio: StateFlow<Track> = _currentSelectedAudio.asStateFlow()

    private val _audioList = MutableStateFlow<List<Track>>(emptyList())
    val audioList: StateFlow<List<Track>> = _audioList.asStateFlow()

    private val _source = MutableStateFlow("")
    val source: StateFlow<String> = _source.asStateFlow()

    private val _uiState = MutableStateFlow<UIState>(UIState.Initial)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    private val _trackArtwork = MutableStateFlow<ByteArray?>(null)
    val trackArtwork: StateFlow<ByteArray?> = _trackArtwork.asStateFlow()

    private var artworkJob: Job? = null

    init {
        restoreStateIfPlaying()
        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                when (mediaState) {
                    is MusicState.Initial -> _uiState.value = UIState.Initial
                    is MusicState.Buffering -> calculateProgressValue(mediaState.progress)
                    is MusicState.CurrentPlaying -> {
                        val track =
                            _audioList.value.getOrNull(mediaState.mediaItemIndex) ?: audioDummy
                        _currentSelectedAudio.value = track
                        loadArtworkForCurrentTrack()
                    }

                    is MusicState.Playing -> _isPlaying.value = mediaState.isPlaying
                    is MusicState.Progress -> calculateProgressValue(mediaState.progress)
                    is MusicState.Ready -> _duration.value = mediaState.duration
                }
            }
        }
    }

    private fun restoreStateIfPlaying() {
        val playlist = audioServiceHandler.restorePlaylist()
        if (playlist.isEmpty()) return
        _audioList.value = playlist
        val currentTrack = playlist.getOrNull(audioServiceHandler.currentMediaItemIndex())
            ?: audioDummy
        _currentSelectedAudio.value = currentTrack
        _source.value =
            if (currentTrack.uri?.scheme == ContentResolver.SCHEME_CONTENT) "local" else "api"
        _isPlaying.value = audioServiceHandler.isCurrentlyPlaying()
        _duration.value = audioServiceHandler.duration()
        calculateProgressValue(audioServiceHandler.currentPosition())
        loadArtworkForCurrentTrack()
        _uiState.value = UIState.Ready
    }

    /**
     * Plays the track requested before the player screen was opened; without a request keeps the current one.
     *
     * @return false when there is nothing to show: no request and an empty queue (e.g. the screen
     * was restored from the back stack after process death), so the screen should close.
     */
    fun onPlayerOpened(): Boolean {
        val request = playerRequests.take() ?: return _audioList.value.isNotEmpty()
        when (request.source) {
            TrackSource.DEEZER -> loadRemoteTrack(request.id)
            TrackSource.LOCAL -> loadLocalTrack(request.id)
        }
        return true
    }

    fun loadRemoteTrack(id: String) {
        if (_currentSelectedAudio.value.id.toString() == id) {
            resumeIfStopped()
            return
        }
        _source.value = "api"
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            getRemoteTrackUseCase(id)
                .onSuccess { track ->
                    _currentSelectedAudio.value = track
                    _audioList.value = listOf(track)
                    loadArtworkForCurrentTrack()
                    track.album?.id?.let { loadRemoteAlbum(it) }
                    setMediaItem()
                    audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
                    _uiState.value = UIState.Ready
                }
                .onError { error -> _uiState.value = UIState.Error(error.toUiText()) }
        }
    }

    private suspend fun loadRemoteAlbum(albumId: Int) {
        getRemoteAlbumUseCase(albumId.toString())
            .onSuccess { albumTracks ->
                val filtered = albumTracks.filter { it.id != _currentSelectedAudio.value.id }
                _audioList.update { it + filtered }
            }
            .onError { error -> _uiState.value = UIState.Error(error.toUiText()) }
    }

    private fun setMediaItem() {
        val mediaItems = _audioList.value.map { audio ->
            MediaItem.Builder()
                .setMediaId(audio.id.toString())
                .setUri(audio.preview)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audio.title)
                        .setArtist(audio.artistName)
                        .setArtworkUri(
                            audio.album?.cover?.takeIf { it.isNotEmpty() }?.toUri()
                                ?: audio.uri
                        )
                        .setExtras(bundleOf(EXTRA_TRACK_DURATION_MS to audio.duration))
                        .build()
                )
                .build()
        }
        audioServiceHandler.setMediaItemList(mediaItems)
    }

    fun loadLocalTrack(trackUri: String) {
        if (_currentSelectedAudio.value.preview == trackUri && _audioList.value.isNotEmpty()) {
            resumeIfStopped()
            return
        }

        viewModelScope.launch {
            _uiState.value = UIState.Loading
            _source.value = "local"

            val tracks = getLocalTrackListUseCase()
            _audioList.value = tracks

            val selectedTrack = tracks.find { it.preview == trackUri }
            if (selectedTrack == null) {
                _uiState.value = UIState.Error(UiText.DynamicString("Трек не найден: $trackUri"))
                return@launch
            }
            _currentSelectedAudio.value = selectedTrack
            setMediaItem()
            loadArtworkForCurrentTrack()
            _uiState.value = UIState.Ready

            audioServiceHandler.onPlayerEvents(
                PlayerEvent.SelectedAudioChange,
                selectedAudioIndex = tracks.indexOf(selectedTrack)
            )
            audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
        }
    }

    /**
     * The same track was tapped again. A paused track stays paused, but a queue left behind by a
     * destroyed service (the player is idle) plays again, or the tap would do nothing.
     */
    private fun resumeIfStopped() {
        if (audioServiceHandler.isIdle()) audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
    }

    private fun loadArtworkForCurrentTrack() {
        artworkJob?.cancel()
        _trackArtwork.value = null
        val track = _currentSelectedAudio.value
        val uri = track.uri ?: return
        artworkJob = viewModelScope.launch {
            _trackArtwork.value = getTrackArtworkUseCase(track.id, uri)
        }
    }


    private fun calculateProgressValue(currentProgress: Long) {
        val duration = _duration.value
        _progress.value =
            if (currentProgress > 0 && duration > 0) (currentProgress.toFloat() / duration.toFloat()) * 100f
            else 0f
        _progressString.value = formatDuration(currentProgress)
    }

    fun onUiEvents(uiEvents: UIEvents) = viewModelScope.launch {
        when (uiEvents) {
            UIEvents.Backward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Backward)
            UIEvents.Forward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Forward)
            UIEvents.SeekToNext -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToNext)
            UIEvents.SeekToPrevious -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToPrevious)
            is UIEvents.PlayPause -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.PlayPause
                )
            }

            is UIEvents.SeekTo -> {
                if (_duration.value <= 0) return@launch
                val seekPosition = ((_duration.value * uiEvents.position) / 100f).toLong()
                audioServiceHandler.onPlayerEvents(PlayerEvent.SeekTo, seekPosition = seekPosition)
                // The progress loop is off while paused: show the new position right away.
                _progress.value = uiEvents.position
                _progressString.value = formatDuration(seekPosition)
            }

            is UIEvents.UpdateProgress -> {
                audioServiceHandler.onPlayerEvents(PlayerEvent.UpdateProgress(uiEvents.newProgress))
                _progress.value = uiEvents.newProgress
            }

            else -> Unit
        }
    }


    @SuppressLint("DefaultLocale")
    fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds =
            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

}