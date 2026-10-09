package com.jayelmeynak.feature.player.api

/**
 * What the player is doing, as a snapshot of the playback session.
 *
 * The playback position is not part of it: it changes every moment and is published separately
 * in [PlaybackController.positionMs].
 */
public sealed interface PlaybackState {

    /** Not connected to the playback session yet (or disconnected while the app is in background). */
    public data object Idle : PlaybackState

    /**
     * Connected to the playback session.
     *
     * @property queue The current queue; empty when nothing was asked to play.
     * @property currentIndex Index of the current item in [queue].
     * @property isPlaying The position is advancing right now.
     * @property playWhenReady Playback is requested: `true` while playing and while buffering
     *   before playing, `false` on pause.
     * @property isBuffering The player is loading the current item and cannot play yet.
     * @property showPlayButton Тап по play/pause запустит воспроизведение — то же правило, что у
     *   [PlaybackController.togglePlayPause]. `true` на паузе, после ошибки, в конце очереди и
     *   когда воспроизведение запрошено, но подавлено (например, потерян аудиофокус:
     *   [playWhenReady] = `true`, а тап возобновит звук); `false`, пока воспроизведение запрошено
     *   и не подавлено, в том числе во время буферизации. Иконку
     *   play/pause рисовать по нему, а не по [isPlaying].
     * @property durationMs Duration of the current item as the player reports it; `0` when unknown.
     * @property error The reason playback stopped, or `null`.
     */
    public data class Active(
        val queue: List<QueueItem>,
        val currentIndex: Int,
        val isPlaying: Boolean,
        val playWhenReady: Boolean,
        val isBuffering: Boolean,
        val showPlayButton: Boolean,
        val durationMs: Long,
        val error: PlaybackError?,
    ) : PlaybackState {

        /** The item at [currentIndex], or `null` if the index is outside [queue]. */
        public val current: QueueItem? get() = queue.getOrNull(currentIndex)
    }
}

/** Why playback failed, without player library types. */
public enum class PlaybackError {
    /** The track could not be loaded: no URI from [TrackUriResolver], network or file error. */
    SOURCE_UNAVAILABLE,

    /** Any other player error. */
    UNKNOWN,
}
