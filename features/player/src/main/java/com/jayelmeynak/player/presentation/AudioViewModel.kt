package com.jayelmeynak.player.presentation

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
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
import com.jayelmeynak.player.domain.repository.MusicRemoteRepository
import com.jayelmeynak.player.player.service.MusicServiceHandler
import com.jayelmeynak.player.player.service.MusicState
import com.jayelmeynak.player.player.service.PlayerEvent
import com.jayelmeynak.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var duration by savedStateHandle.saveable { mutableStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableStateOf(0f) }
    var progressString by savedStateHandle.saveable { mutableStateOf("00:00") }
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false) }
    var currentSelectedAudio by savedStateHandle.saveable { mutableStateOf(audioDummy) }
    var audioList by savedStateHandle.saveable { mutableStateOf(listOf<Track>()) }

    private val _uiState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                Log.d("MyLog","CurrentMediaState $mediaState")
                when (mediaState) {
                    is MusicState.Initial -> _uiState.value = UIState.Initial
                    is MusicState.Buffering -> calculateProgressValue(mediaState.progress)
                    is MusicState.Playing -> isPlaying = mediaState.isPlaying
                    is MusicState.Progress -> calculateProgressValue(mediaState.progress)
                    is MusicState.CurrentPlaying -> {
                        val track = audioList.getOrNull(mediaState.mediaItemIndex) ?: audioDummy
                        currentSelectedAudio = track

                        Log.d("MyLog", "init ${audioList}")
                    }
                    is MusicState.Ready -> {
                        duration = mediaState.duration
                        _uiState.value = UIState.Ready
                    }
                }
            }
        }
    }

    fun loadRemoteTrack(id: String) {
        Log.d("MyLog","loadRemoteTrack ${audioList}")
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val result = withContext(Dispatchers.IO) { musicRemoteRepository.getTrack(id) }
            result.onSuccess { track ->
                withContext(Dispatchers.Main) {
                    currentSelectedAudio = track
                    audioList = listOf(track)
                }
                track.album?.id?.let { loadRemoteAlbum(it) }
                withContext(Dispatchers.Main) {
                    if (audioList.size == 1) {
                        setMediaItem()
                    }
                    _uiState.value = UIState.Ready
                    audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
                }
            }.onError { error ->
                withContext(Dispatchers.Main) {
                    _uiState.value = UIState.Error(error.toUiText())
                }
            }
        }
    }

    private fun loadRemoteAlbum(albumId: Int) {
        Log.d("MyLog","loadRemoteAlbum ${audioList}")
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val result = withContext(Dispatchers.IO) {
                musicRemoteRepository.getAlbum(albumId.toString())
            }
            result.onSuccess { albumTracks ->
                val filteredTracks = albumTracks.filter { it.id != currentSelectedAudio.id }
                currentSelectedAudio.let { track ->
                    audioList = audioList + filteredTracks
                }
                setMediaItem()
            }
                .onError { error ->
                    _uiState.value = UIState.Error(error.toUiText())
                }
        }
    }

    private fun setMediaItem() {
        audioList.map { audio ->
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

    fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        val contentResolver: ContentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    fun getFileMetadata(uri: Uri): Pair<String?, String?> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val artistName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val fileId = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        retriever.release()
        return Pair(fileId, artistName)
    }

    fun loadLocalTrack(trackUri: String) {
        Log.d("MyLog","loadLocalTrack ${audioList}")
        if (audioList.any { it.preview == trackUri }) {
            Log.d("MyLog", "Track already loaded, skipping loadLocalTrack")
            return
        }
        viewModelScope.launch {
            val track = withContext(Dispatchers.IO) {
                val uri = Uri.parse(trackUri)
                val fileName = getFileNameFromUri(uri) ?: "Unknown Title"
                val (fileId, artistName) = getFileMetadata(uri)
                Track(
                    id = fileId?.toLongOrNull() ?: 0,
                    title = fileName,
                    artistName = artistName ?: "Unknown Artist",
                    preview = trackUri,
                    album = null,
                    uri = uri
                )
            }
            currentSelectedAudio = track
            audioList = listOf(track)
            setMediaItem()
            audioServiceHandler.onPlayerEvents(PlayerEvent.PlayPause)
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

            is UIEvents.SelectedAudioChange -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.SelectedAudioChange,
                    selectedAudioIndex = uiEvents.index
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