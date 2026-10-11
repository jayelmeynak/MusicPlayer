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
     * Position in the current item in milliseconds. Updated while playing, and once after a seek
     * on pause and after the current item changes; [play] resets it to `0`.
     */
    public val positionMs: StateFlow<Long>

    /**
     * Replaces the queue with [queue] and starts playing the item at [startIndex] from the start.
     * If [queue] already is the current queue and [startIndex] the current item, the item is not
     * restarted: it keeps playing, or resumes if paused (a tap on the playing track in a list).
     *
     * @throws IllegalArgumentException if [startIndex] is not in `queue.indices` (so [queue] must
     *   not be empty).
     */
    public fun play(queue: List<QueueItem>, startIndex: Int)

    public fun togglePlayPause()

    public fun seekTo(positionMs: Long)

    public fun next()

    public fun previous()

    public fun seekBack()

    public fun seekForward()
}
