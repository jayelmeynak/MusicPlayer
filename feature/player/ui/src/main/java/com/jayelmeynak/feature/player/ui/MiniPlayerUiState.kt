package com.jayelmeynak.feature.player.ui

/**
 * Мини-плеер поверх содержимого вкладки.
 *
 * @property showPlayButton Тап по play/pause запустит воспроизведение; иконка рисуется по нему.
 * @property progress Позиция в процентах от длительности текущего элемента; `0`, если она неизвестна.
 * @property trackKey Меняется с текущим элементом; сбрасывает незаконченный жест перемотки.
 * @property canSeek Ползунок перемотки активен: сессия примет перемотку, длительность известна.
 * @property visible В снапшоте сессии есть свой текущий трек — в том числе в последнем снапшоте
 *   без подключения (приложение возвращается из фона): так мини-плеер не мигает.
 */
internal data class MiniPlayerUiState(
    val title: String = "",
    val artist: String = "",
    val showPlayButton: Boolean = true,
    val progress: Float = 0f,
    val trackKey: String = "",
    val canSeek: Boolean = false,
    val visible: Boolean = false,
)
