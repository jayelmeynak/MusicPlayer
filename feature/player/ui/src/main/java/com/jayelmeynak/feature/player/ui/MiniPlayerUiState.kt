package com.jayelmeynak.feature.player.ui

/**
 * The mini player over the tab content.
 *
 * @property showPlayButton Тап по play/pause запустит воспроизведение; иконка рисуется по нему.
 * @property progress Position in percent of the current item's duration; `0` when it is unknown.
 * @property trackKey Changes with the current item; drops an unfinished seek gesture.
 * @property visible В снапшоте сессии есть свой текущий трек — в том числе в последнем снапшоте
 *   без подключения (приложение возвращается из фона): так мини-плеер не мигает.
 */
internal data class MiniPlayerUiState(
    val title: String = "",
    val artist: String = "",
    val showPlayButton: Boolean = true,
    val progress: Float = 0f,
    val trackKey: String = "",
    val visible: Boolean = false,
)
