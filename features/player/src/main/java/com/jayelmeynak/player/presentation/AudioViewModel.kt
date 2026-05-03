package com.jayelmeynak.player.presentation

import android.annotation.SuppressLint
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.jayelmeynak.local.domain.usecase.GetTrackArtworkUseCase
import com.jayelmeynak.network.utils.onError
import com.jayelmeynak.network.utils.onSuccess
import com.jayelmeynak.player.domain.models.Album
import com.jayelmeynak.player.domain.models.Track
import com.jayelmeynak.player.domain.useсase.GetLocalTrackListUseCase
import com.jayelmeynak.player.domain.useсase.GetRemoteAlbumUseCase
import com.jayelmeynak.player.domain.useсase.GetRemoteTrackUseCase
import com.jayelmeynak.player.player.service.MusicServiceHandler
import com.jayelmeynak.player.player.service.MusicState
import com.jayelmeynak.player.player.service.PlayerEvent
import com.jayelmeynak.ui.UiText
import com.jayelmeynak.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
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
class AudioViewModel @Inject constructor(
    private val audioServiceHandler: MusicServiceHandler,
    private val getLocalTrackListUseCase: GetLocalTrackListUseCase,
    private val getRemoteTrackUseCase: GetRemoteTrackUseCase,
    private val getRemoteAlbumUseCase: GetRemoteAlbumUseCase,
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
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
                        _trackArtwork.value = null
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
        _currentSelectedAudio.value =
            playlist.getOrNull(audioServiceHandler.currentMediaItemIndex())
            ?: audioDummy
        _isPlaying.value = audioServiceHandler.isCurrentlyPlaying()
        _duration.value = audioServiceHandler.duration()
        calculateProgressValue(audioServiceHandler.currentPosition())
        loadArtworkForCurrentTrack()
        _uiState.value = UIState.Ready
    }

    fun loadRemoteTrack(id: String) {
        if (_currentSelectedAudio.value.id.toString() == id) return
        _source.value = "api"
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            getRemoteTrackUseCase(id)
                .onSuccess { track ->
                    _currentSelectedAudio.value = track
                    _audioList.value = listOf(track)
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
                        .build()
                )
                .build()
        }
        audioServiceHandler.setMediaItemList(mediaItems)
    }

    fun loadLocalTrack(trackUri: String) {
        if (_currentSelectedAudio.value.preview == trackUri && _audioList.value.isNotEmpty()) return

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

    private fun loadArtworkForCurrentTrack() {
        val track = _currentSelectedAudio.value
        val uri = track.uri ?: return
        viewModelScope.launch {
            _trackArtwork.value = getTrackArtworkUseCase(track.id, uri)
        }
    }


    private fun calculateProgressValue(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0) ((currentProgress.toFloat() / _duration.value.toFloat()) * 100f)
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
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.SeekTo,
                    seekPosition = ((_duration.value * uiEvents.position) / 100f).toLong()
                )
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