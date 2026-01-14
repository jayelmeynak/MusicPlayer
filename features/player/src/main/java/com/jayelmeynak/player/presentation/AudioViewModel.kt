package com.jayelmeynak.player.presentation

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.jayelmeynak.network.utils.onError
import com.jayelmeynak.network.utils.onSuccess
import com.jayelmeynak.player.domain.models.Album
import com.jayelmeynak.player.domain.models.Track
import com.jayelmeynak.player.domain.repository.MusicLocalRepository
import com.jayelmeynak.player.domain.repository.MusicRemoteRepository
import com.jayelmeynak.player.player.service.MusicServiceHandler
import com.jayelmeynak.player.player.service.MusicState
import com.jayelmeynak.player.player.service.PlayerEvent
import com.jayelmeynak.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val musicRemoteRepository: MusicRemoteRepository,
    private val musicLocalRepository: MusicLocalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var duration by savedStateHandle.saveable { mutableStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableStateOf(0f) }
    var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }
    var currentSelectedAudio by savedStateHandle.saveable { mutableStateOf(audioDummy) }

    // НЕ сохраняем audioList в SavedStateHandle - он слишком большой
    var audioList = mutableStateOf(listOf<Track>())
        private set

    var source by savedStateHandle.saveable { mutableStateOf("") }

    // Сохраняем ID текущего трека для восстановления
    private var savedTrackId by savedStateHandle.saveable { mutableStateOf("") }

    private val _uiState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    init {
        Log.d("MyLog", "AudioViewModel created: ${this.hashCode()}")

        // Восстановление состояния при пересоздании
        restoreStateIfNeeded()

        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                Log.d("MyLog", "[VM:${this@AudioViewModel.hashCode()}] CurrentMediaState $mediaState")
                when (mediaState) {
                    is MusicState.Initial -> _uiState.value = UIState.Initial
                    is MusicState.Buffering -> calculateProgressValue(mediaState.progress)
                    is MusicState.CurrentPlaying -> currentSelectedAudio =
                        audioList.value.getOrNull(mediaState.mediaItemIndex) ?: audioDummy
                    is MusicState.Playing -> isPlaying = mediaState.isPlaying
                    is MusicState.Progress -> calculateProgressValue(mediaState.progress)
                    is MusicState.Ready -> {
                        duration = mediaState.duration
                    }
                }
            }
        }
    }

    private fun restoreStateIfNeeded() {
        // Если есть сохраненный трек и audioList пуст - восстанавливаем
        if (savedTrackId.isNotEmpty() && audioList.value.isEmpty()) {
            Log.d("MyLog", "Restoring state for track: $savedTrackId, source: $source")
            when (source) {
                "api" -> loadRemoteTrack(savedTrackId, skipIfSame = false)
                "local" -> loadLocalTrack(savedTrackId, skipIfSame = false)
            }
        }
    }

    fun loadRemoteTrack(id: String, skipIfSame: Boolean = true) {
        if (skipIfSame && currentSelectedAudio.id.toString() == id) {
            return
        }
        source = "api"
        savedTrackId = id // Сохраняем для восстановления
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val result = withContext(Dispatchers.IO) { musicRemoteRepository.getTrack(id) }
            result.onSuccess { track ->
                withContext(Dispatchers.Main) {
                    currentSelectedAudio = track
                    audioList = mutableStateOf(listOf(track))
                }
                track.album?.id?.let { loadRemoteAlbum(it) }
                withContext(Dispatchers.Main) {
                    if (audioList.value.size == 1) {
                        setMediaItem()
                    }
                    audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
                    _uiState.value = UIState.Ready
                }
            }.onError { error ->
                withContext(Dispatchers.Main) {
                    _uiState.value = UIState.Error(error.toUiText())
                }
            }
        }
    }

    private fun loadRemoteAlbum(albumId: Int) {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val result = withContext(Dispatchers.IO) {
                musicRemoteRepository.getAlbum(albumId.toString())
            }
            result.onSuccess { albumTracks ->
                val filteredTracks = albumTracks.filter { it.id != currentSelectedAudio.id }
                currentSelectedAudio.let { track ->
                    audioList = mutableStateOf(audioList.value + filteredTracks)
                }
                setMediaItem()
            }
                .onError { error ->
                    _uiState.value = UIState.Error(error.toUiText())
                }
        }
    }

    private fun setMediaItem() {
        audioList.value.map { audio ->
            MediaItem.Builder()
                .setUri(audio.preview)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audio.title)
                        .setArtist(audio.artistName)
                        .setArtworkUri(Uri.parse(audio.album?.cover ?: "") ?: Uri.EMPTY)
                        .build()
                )
                .build()
        }.also {
            audioServiceHandler.setMediaItemList(it)
        }
    }

    fun loadLocalTrack(trackUri: String, skipIfSame: Boolean = true) {
        if (skipIfSame && currentSelectedAudio.preview == trackUri) {
            return
        }
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            source = "local"
            savedTrackId = trackUri // Сохраняем для восстановления
            audioList = mutableStateOf(musicLocalRepository.getTracksList())
            setMediaItem()
            currentSelectedAudio = audioList.value.find { it.preview == trackUri } ?: audioDummy
            val currentTrackIndex = audioList.value.indexOf(currentSelectedAudio)
            _uiState.value = UIState.Ready
            audioServiceHandler.onPlayerEvents(
                PlayerEvent.SelectedAudioChange,
                selectedAudioIndex = currentTrackIndex
            )
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
        super.onCleared()
        Log.d("MyLog", "AudioViewModel cleared: ${this.hashCode()}")
        viewModelScope.launch {
            audioServiceHandler.onPlayerEvents(PlayerEvent.Stop)
        }
    }
}