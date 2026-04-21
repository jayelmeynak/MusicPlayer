package com.jayelmeynak.player.presentation

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
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
import com.jayelmeynak.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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


@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioServiceHandler: MusicServiceHandler,
    private val getLocalTrackListUseCase: GetLocalTrackListUseCase,
    private val getRemoteTrackUseCase: GetRemoteTrackUseCase,
    private val getRemoteAlbumUseCase: GetRemoteAlbumUseCase,
    private val getTrackArtworkUseCase: GetTrackArtworkUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var duration by savedStateHandle.saveable { mutableStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableStateOf(0f) }
    var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }
    var currentSelectedAudio by savedStateHandle.saveable { mutableStateOf(audioDummy) }
    var audioList by savedStateHandle.saveable { mutableStateOf(listOf<Track>()) }
    var source by savedStateHandle.saveable { mutableStateOf("") }

    private val _uiState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
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
                    is MusicState.CurrentPlaying -> currentSelectedAudio =
                        audioList.getOrNull(mediaState.mediaItemIndex) ?: audioDummy
                    is MusicState.Playing -> isPlaying = mediaState.isPlaying
                    is MusicState.Progress -> calculateProgressValue(mediaState.progress)
                    is MusicState.Ready -> {
                        duration = mediaState.duration
                    }
                }
            }
        }
    }

    private fun restoreStateIfPlaying() {
        val playlist = audioServiceHandler.restorePlaylist()
        if (playlist.isEmpty()) return
        audioList = playlist
        currentSelectedAudio = playlist.getOrNull(audioServiceHandler.currentMediaItemIndex())
            ?: audioDummy
        isPlaying = audioServiceHandler.isCurrentlyPlaying()
        duration = audioServiceHandler.duration()
        calculateProgressValue(audioServiceHandler.currentPosition())
        loadArtworkForCurrentTrack()
        _uiState.value = UIState.Ready
    }

    fun loadRemoteTrack(id: String) {
        if (currentSelectedAudio.id.toString() == id) {
            return
        }
        source = "api"
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val result = getRemoteTrackUseCase(id)
            result.onSuccess { track ->
                currentSelectedAudio = track
                audioList = listOf(track)
                track.album?.id?.let { loadRemoteAlbum(it) }
                setMediaItem()
                audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
                _uiState.value = UIState.Ready
            }.onError { error ->
                _uiState.value = UIState.Error(error.toUiText())
            }
        }
    }

    private suspend fun loadRemoteAlbum(albumId: Int) {
        val result = getRemoteAlbumUseCase(albumId.toString())
        result.onSuccess { albumTracks ->
            val filteredTracks = albumTracks.filter { it.id != currentSelectedAudio.id }
            audioList = audioList + filteredTracks
        }.onError { error ->
            _uiState.value = UIState.Error(error.toUiText())
        }
    }

    private fun setMediaItem() {
        audioList.map { audio ->
            MediaItem.Builder()
                .setMediaId(audio.id.toString())
                .setUri(audio.preview)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audio.title)
                        .setArtist(audio.artistName)
                        .setArtworkUri(
                            audio.album?.cover?.takeIf { it.isNotEmpty() }
                                ?.let { Uri.parse(it) }
                                ?: audio.uri
                        )
                        .build()
                )
                .build()
        }.also {
            audioServiceHandler.setMediaItemList(it)
        }
    }

    fun loadLocalTrack(trackUri: String) {
        if (currentSelectedAudio.preview == trackUri) {
            return
        }
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            source = "local"
            audioList = getLocalTrackListUseCase()
            setMediaItem()
            currentSelectedAudio = audioList.find { it.preview == trackUri } ?: audioDummy
            val currentTrackIndex = audioList.indexOf(currentSelectedAudio)
            loadArtworkForCurrentTrack()
            _uiState.value = UIState.Ready
            audioServiceHandler.onPlayerEvents(
                PlayerEvent.SelectedAudioChange,
                selectedAudioIndex = currentTrackIndex
            )
        }
    }

    private fun loadArtworkForCurrentTrack() {
        val uri = currentSelectedAudio.uri ?: return
        val trackId = currentSelectedAudio.id
        viewModelScope.launch {
            _trackArtwork.value = getTrackArtworkUseCase(trackId, uri)
        }
    }


    private fun calculateProgressValue(currentProgress: Long) {
        progress =
            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f)
            else 0f
        progressString = formatDuration(currentProgress)
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
                    seekPosition = ((duration * uiEvents.position) / 100f).toLong()
                )
            }

            is UIEvents.UpdateProgress -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.UpdateProgress(
                        uiEvents.newProgress
                    )
                )
                progress = uiEvents.newProgress
            }

            else -> {

            }
        }
    }


    @SuppressLint("DefaultLocale")
    fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds =
            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        viewModelScope.launch {
            audioServiceHandler.onPlayerEvents(PlayerEvent.Stop)
        }
        super.onCleared()
    }
}