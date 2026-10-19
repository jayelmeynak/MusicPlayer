package com.jayelmeynak.feature.player.api

/**
 * Что делает плеер — снапшот сессии воспроизведения.
 *
 * Позиции в нём нет: она меняется каждый миг и публикуется отдельно в
 * [PlaybackController.positionMs].
 */
public sealed interface PlaybackState {

    /** Снапшота сессии ещё не было: контроллер не подключался с начала процесса. */
    public data object Idle : PlaybackState

    /**
     * Подключиться к сессии не удалось после всех попыток. Команды игнорируются; следующий выход
     * приложения на передний план пробует подключиться снова.
     */
    public data object ConnectionFailed : PlaybackState

    /**
     * Подключено к сессии воспроизведения.
     *
     * @property queue Текущая очередь; пустая, если ничего не просили играть.
     * @property currentIndex Индекс текущего элемента в [queue].
     * @property isPlaying Позиция идёт прямо сейчас.
     * @property playWhenReady Воспроизведение запрошено: `true` во время игры и буферизации перед
     *   ней, `false` на паузе.
     * @property isBuffering Плеер загружает текущий элемент и пока не может играть.
     * @property showPlayButton Тап по play/pause запустит воспроизведение — то же правило, что у
     *   [PlaybackController.togglePlayPause]. `true` на паузе, после ошибки, в конце очереди и
     *   когда воспроизведение запрошено, но подавлено (например, потерян аудиофокус:
     *   [playWhenReady] = `true`, а тап возобновит звук); `false`, пока воспроизведение запрошено
     *   и не подавлено, в том числе во время буферизации. Иконку
     *   play/pause рисовать по нему, а не по [isPlaying].
     * @property durationMs Длительность текущего элемента по данным плеера; `0`, если неизвестна.
     * @property error Причина остановки воспроизведения или `null`.
     * @property canSeek Сессия примет [PlaybackController.seekTo]: текущий трек подготовлен и
     *   перематывается. `false` до подготовки трека и у источника без перемотки (например, MP3 с
     *   переменным битрейтом без таблицы перемотки) — тогда `seekTo` игнорируется.
     * @property canSeekBack Сессия примет [PlaybackController.seekBack].
     * @property canSeekForward Сессия примет [PlaybackController.seekForward].
     * @property isConnected `false` — последний известный снапшот, контроллер сейчас не подключён
     *   (приложение в фоне, сессия переподключается или ещё восстанавливает очередь): показывать
     *   можно; команды игнорируются, пока нет подключения (см. [PlaybackController]).
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
        val canSeek: Boolean,
        val canSeekBack: Boolean,
        val canSeekForward: Boolean,
        val isConnected: Boolean = true,
    ) : PlaybackState {

        /** Элемент [currentIndex] или `null`, если индекс вне [queue]. */
        public val current: QueueItem? get() = queue.getOrNull(currentIndex)
    }
}

/** Почему воспроизведение упало — без типов библиотеки плеера. */
public enum class PlaybackError {
    /**
     * Трек не загрузился: резолвер не дал ссылку ([TrackUriResolver] вернул `null`, в том числе без
     * сети), сервер ответил ошибкой, файла нет.
     */
    SOURCE_UNAVAILABLE,

    /** Поток трека не загрузился из-за сети: нет подключения или истекло ожидание соединения. */
    NETWORK,

    /** Любая другая ошибка плеера. */
    UNKNOWN,
}
