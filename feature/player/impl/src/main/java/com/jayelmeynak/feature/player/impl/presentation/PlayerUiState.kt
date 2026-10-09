package com.jayelmeynak.feature.player.impl.presentation

import com.jayelmeynak.lib.designsystem.UiText

internal sealed interface PlayerUiState {

    /** Not connected to the playback session yet: nothing to show but a progress indicator. */
    data object Connecting : PlayerUiState

    /**
     * The current track.
     *
     * @property trackKey Changes with the current item; drops an unfinished seek gesture.
     * @property artworkUri Cover image URL (Deezer); `null` for a local track.
     * @property artworkData Cover of a local track from the artwork cache, while it is loaded.
     * @property isPreview The media is a 30-second Deezer preview, not the full track.
     * @property trackLengthMs Full length of the track; `0` when unknown.
     * @property durationMs Length of what is playing, from the player; `0` when unknown.
     * @property showPlayButton Тап по play/pause запустит воспроизведение; иконка рисуется по нему.
     * @property isBuffering Текущий элемент загружается: кольцо прогресса вокруг play/pause.
     * @property progress Position in percent of [durationMs]; `0` when the duration is unknown.
     */
    data class Ready(
        val trackKey: String,
        val title: String,
        val artist: String,
        val artworkUri: String?,
        val artworkData: ByteArray?,
        val isPreview: Boolean,
        val trackLengthMs: Long,
        val showPlayButton: Boolean,
        val isBuffering: Boolean,
        val durationMs: Long,
        val positionMs: Long,
        val errorMessage: UiText?,
    ) : PlayerUiState {

        val progress: Float
            get() = if (durationMs > 0 && positionMs > 0) {
                (positionMs.toFloat() / durationMs * PERCENT).coerceIn(0f, PERCENT)
            } else {
                0f
            }
    }
}

private const val PERCENT = 100f
