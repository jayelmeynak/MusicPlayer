package com.jayelmeynak.feature.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Управляет сессией воспроизведения, которой владеет сервис плеера.
 *
 * [state] — [PlaybackState.Idle], пока контроллер ни разу не получил снапшот сессии. [play] до
 * подключения исполняется после него (выигрывает последний вызов); в
 * [PlaybackState.ConnectionFailed] он ещё и начинает новую серию попыток подключения. Транспортные
 * команды ([togglePlayPause], [seekTo], [next], [previous], [seekBack], [seekForward]) без
 * подключения игнорируются: до первого подключения, в [PlaybackState.ConnectionFailed] и в
 * снапшоте [PlaybackState.Active] с `isConnected = false`. Пока подключённая сессия поднимает
 * сохранённую очередь, [state] ещё прежний ([PlaybackState.Idle] или снапшот с
 * `isConnected = false`), а команды уже доходят до сессии: play возобновит сохранённую очередь.
 *
 * Все методы вызываются только на главном потоке; на нём же реализация создаётся.
 */
public interface PlaybackController {

    public val state: StateFlow<PlaybackState>

    /**
     * Позиция в текущем элементе, мс. Обновляется во время воспроизведения и один раз после
     * перемотки на паузе и смены текущего элемента; [play] сбрасывает её в `0`.
     */
    public val positionMs: StateFlow<Long>

    /**
     * Заменяет очередь на [queue] и играет элемент [startIndex] с начала. Если [queue] — уже
     * текущая очередь, а [startIndex] — текущий элемент, он не перезапускается: играет дальше или
     * возобновляется с паузы (тап по играющему треку в списке).
     *
     * @throws IllegalArgumentException если [startIndex] не в `queue.indices` (значит, [queue]
     *   не может быть пустой).
     */
    public fun play(queue: List<QueueItem>, startIndex: Int)

    public fun togglePlayPause()

    /**
     * Перематывает текущий элемент. Если он не перематывается (ещё не подготовлен, источник без
     * перемотки), вызов игнорируется и [positionMs] не меняется.
     */
    public fun seekTo(positionMs: Long)

    public fun next()

    public fun previous()

    public fun seekBack()

    public fun seekForward()
}
