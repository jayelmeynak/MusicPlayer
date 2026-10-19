package com.jayelmeynak.feature.player.impl.presentation

import com.jayelmeynak.feature.player.api.progressPercent
import com.jayelmeynak.lib.designsystem.UiText

internal sealed interface PlayerUiState {

    /** Ещё нет подключения к сессии воспроизведения: показать нечего, кроме индикатора прогресса. */
    data object Connecting : PlayerUiState

    /** Подключиться к сессии не удалось после всех попыток: текст ошибки вместо индикатора. */
    data object ConnectionFailed : PlayerUiState

    /**
     * Текущий трек.
     *
     * @property trackKey Меняется с текущим элементом; сбрасывает незаконченный жест перемотки.
     * @property artworkUri Обложка: адрес картинки (Deezer) или `content://` URI, обложка которого —
     *   эскиз провайдера (локальный трек); `null` — обложки нет.
     * @property isPreview Играет 30-секундное превью Deezer, а не полный трек.
     * @property trackLengthMs Полная длина трека; `0`, если неизвестна.
     * @property durationMs Длина того, что играет, по данным плеера; `0`, если неизвестна.
     * @property showPlayButton Тап по play/pause запустит воспроизведение; иконка рисуется по нему.
     * @property isBuffering Текущий элемент загружается: кольцо прогресса вокруг play/pause.
     * @property canSeek Ползунок перемотки активен: сессия примет перемотку, длительность известна.
     * @property canSeekBack Кнопка перемотки назад активна.
     * @property canSeekForward Кнопка перемотки вперёд активна.
     * @property progress Позиция в процентах от [durationMs]; `0`, если длительность неизвестна.
     */
    data class Ready(
        val trackKey: String,
        val title: String,
        val artist: String,
        val artworkUri: String?,
        val isPreview: Boolean,
        val trackLengthMs: Long,
        val showPlayButton: Boolean,
        val isBuffering: Boolean,
        val durationMs: Long,
        val positionMs: Long,
        val errorMessage: UiText?,
        val canSeek: Boolean,
        val canSeekBack: Boolean,
        val canSeekForward: Boolean,
    ) : PlayerUiState {

        val progress: Float
            get() = progressPercent(positionMs, durationMs)
    }
}
