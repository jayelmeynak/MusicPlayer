package com.jayelmeynak.feature.player.ui

/**
 * The mini player over the tab content.
 *
 * @property progress Position in percent of the current item's duration; `0` when it is unknown.
 * @property trackKey Changes with the current item; drops an unfinished seek gesture.
 * @property visible Connected to the playback session and something is in the queue.
 */
internal data class MiniPlayerUiState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val trackKey: String = "",
    val visible: Boolean = false,
)
