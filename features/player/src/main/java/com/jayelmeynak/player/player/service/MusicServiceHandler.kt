package com.jayelmeynak.player.player.service

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var progressJob: Job? = null

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

    suspend fun onPlayerEvents(
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
            is PlayerEvent.Stop -> stopProgressUpdate()
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
                        _audioState.value = MusicState.Playing(
                            isPlaying = true
                        )
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
            else -> {

            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Log.d("MyLog", "IsPlaying: $isPlaying")
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
        // Отменяем предыдущий job если он был
        stopProgressUpdate()

        progressJob = scope.launch {
            Log.d("MyLog", "Progress update job started")
            while (true) {
                delay(500)
                _audioState.value = MusicState.Progress(exoPlayer.currentPosition)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
        Log.d("MyLog", "Progress update job stopped")
    }

    /**
     * Очистка ресурсов и отмена всех корутин.
     * Должен вызываться из PlayBackService.onDestroy()
     */
    fun onDestroy() {
        stopProgressUpdate()
        job.cancel()
        exoPlayer.removeListener(this)
        Log.d("MyLog", "MusicServiceHandler destroyed, all coroutines cancelled")
    }
}