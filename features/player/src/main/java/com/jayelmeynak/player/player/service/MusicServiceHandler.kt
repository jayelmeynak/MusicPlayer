package com.jayelmeynak.player.player.service

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jayelmeynak.player.domain.models.Album
import com.jayelmeynak.player.domain.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MusicServiceHandler @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val applicationScope: CoroutineScope,
) : Player.Listener {
    private val _audioState: MutableStateFlow<MusicState> =
        MutableStateFlow(MusicState.Initial)
    val audioState: StateFlow<MusicState> = _audioState.asStateFlow()

    private var job: Job? = null

    init {
        exoPlayer.addListener(this)
    }

    fun setMediaItemList(mediaItems: List<MediaItem>) {
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    fun restorePlaylist(): List<Track> = (0 until exoPlayer.mediaItemCount).map { i ->
        val item = exoPlayer.getMediaItemAt(i)
        val meta = item.mediaMetadata
        val artworkUri = meta.artworkUri?.toString().orEmpty()
        Track(
            id = item.mediaId.toLongOrNull() ?: i.toLong(),
            title = meta.title?.toString().orEmpty(),
            artistName = meta.artist?.toString().orEmpty(),
            preview = item.localConfiguration?.uri?.toString().orEmpty(),
            album = Album(0, "", artworkUri, "", ""),
            uri = item.localConfiguration?.uri?.toString()?.toUri(),
        )
    }

    fun currentMediaItemIndex(): Int = exoPlayer.currentMediaItemIndex

    fun currentPosition(): Long = exoPlayer.currentPosition

    fun duration(): Long =
        exoPlayer.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L

    fun isCurrentlyPlaying(): Boolean = exoPlayer.isPlaying

    fun onPlayerEvents(
        playerEvent: PlayerEvent,
        selectedAudioIndex: Int = -1,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            is PlayerEvent.Backward -> exoPlayer.seekBack()
            is PlayerEvent.Forward -> exoPlayer.seekForward()
            is PlayerEvent.SeekToNext -> exoPlayer.seekToNext()
            is PlayerEvent.SeekToPrevious -> exoPlayer.seekToPrevious()
            is PlayerEvent.PlayPause -> playOrPause()
            is PlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            is PlayerEvent.UpdateProgress -> {
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
            }
            is PlayerEvent.SelectedAudioChange -> {
                when (selectedAudioIndex) {
                    exoPlayer.currentMediaItemIndex -> {
                        playOrPause()
                    }
                    else -> {
                        exoPlayer.seekToDefaultPosition(selectedAudioIndex)
                        _audioState.value = MusicState.Playing(isPlaying = true)
                        exoPlayer.playWhenReady = true
                        startProgressUpdate()
                    }
                }
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                MusicState.Buffering(exoPlayer.currentPosition)
            ExoPlayer.STATE_READY -> _audioState.value =
                MusicState.Ready(exoPlayer.duration)
            else -> Unit
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            _audioState.value = MusicState.Playing(isPlaying = false)
            stopProgressUpdate()
            return
        }
        _audioState.value = MusicState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
        _audioState.value = MusicState.Playing(isPlaying = true)
        startProgressUpdate()
    }

    private fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            return
        }
        exoPlayer.play()
    }

    private fun startProgressUpdate() {
        job?.cancel()
        job = applicationScope.launch(Dispatchers.Main) {
            while (true) {
                delay(500)
                _audioState.value = MusicState.Progress(exoPlayer.currentPosition)
            }
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        job = null
    }

    fun release() {
        stopProgressUpdate()
        exoPlayer.removeListener(this)
    }
}
