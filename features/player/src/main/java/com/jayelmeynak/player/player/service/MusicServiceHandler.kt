package com.jayelmeynak.player.player.service

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MusicServiceHandler @Inject constructor(
    private val exoPlayer: ExoPlayer,
) : Player.Listener {
    private val _audioState: MutableStateFlow<MusicState> =
        MutableStateFlow(MusicState.Initial)
    val audioState: StateFlow<MusicState> = _audioState.asStateFlow()

    private var job: Job? = null

    init {
        exoPlayer.addListener(this)
    }

//    fun addMediaItem(mediaItem: MediaItem) {
//        exoPlayer.setMediaItem(mediaItem)
//        exoPlayer.prepare()
//    }

    fun setMediaItemList(mediaItems: List<MediaItem>) {
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    fun onPlayerEvents(
        playerEvent: PlayerEvent,
        seekPosition: Long = 0,
    ) {
        when (playerEvent) {
            is PlayerEvent.Backward -> exoPlayer.seekBack()
            is PlayerEvent.Forward -> exoPlayer.seekForward()
            is PlayerEvent.SeekToNext -> exoPlayer.seekToNext()
            is PlayerEvent.SeekToPrevious -> exoPlayer.seekToPrevious()
            is PlayerEvent.PlayPause -> playOrPause()
            is PlayerEvent.SeekTo -> exoPlayer.seekTo(seekPosition)
            is PlayerEvent.Stop -> stopProgressUpdate()
            is PlayerEvent.UpdateProgress -> {
                exoPlayer.seekTo(
                    (exoPlayer.duration * playerEvent.newProgress).toLong()
                )
            }
            else -> {
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> _audioState.value =
                MusicState.Buffering(exoPlayer.currentPosition)

            ExoPlayer.STATE_READY -> _audioState.value =
                MusicState.Ready(exoPlayer.duration)
            else -> {

            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            _audioState.value = MusicState.Playing(
                isPlaying = false
            )
            stopProgressUpdate()
            return
        }
        _audioState.value = MusicState.CurrentPlaying(exoPlayer.currentMediaItemIndex)
        _audioState.value = MusicState.Playing(
            isPlaying = true
        )
        job = GlobalScope.launch(Dispatchers.Main) {
            startProgressUpdate()
        }
    }

    private fun playOrPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            return
        }
        exoPlayer.play()
    }



    private suspend fun startProgressUpdate() {
        Log.d("MyLog", "Job running")
        while (true) {
            delay(500)
            _audioState.value = MusicState.Progress(exoPlayer.currentPosition)
        }
    }

    private fun stopProgressUpdate() {
        Log.d("MyLog", "Job ID ${job?.key}")
        job?.cancel()
    }
}