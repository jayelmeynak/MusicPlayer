package com.jayelmeynak.feature.player.api.testing

import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Общий контракт [PlaybackController]: один набор тестов прогоняется на [FakePlaybackController] и на
 * настоящей реализации, чтобы тесты экранов на фейке не расходились с продуктом.
 *
 * Наследник отдаёт ещё не подключённый [controller] и умеет его подключать ([connect] — до первого
 * подключённого снапшота) и уводить в фон ([disconnect]). Проверки — только того, что видно через
 * [PlaybackController] сразу после вызова, без ожидания событий: доставку команд до плеера проверяет
 * тест реализации. Чего здесь нет — `isPlaying` (у настоящего контроллера зависит от буферизации) и
 * изменения состояния транспортными командами (у фейка его задаёт тест).
 */
public abstract class PlaybackControllerContract {

    /** Контроллер теста; к началу теста ещё не подключён. */
    protected abstract val controller: PlaybackController

    /** Подключает [controller] и возвращается, когда [PlaybackController.state] — подключённый снапшот. */
    protected abstract fun connect()

    /** Уход приложения в фон: [controller] теряет подключение. */
    protected abstract fun disconnect()

    /**
     * Ждёт, пока текущий трек подготовлен и перемотка принимается. Настоящий контроллер до этого
     * отбрасывает `seekTo`, не меняя позицию; фейку ждать нечего.
     */
    protected open fun awaitSeekable() {}

    protected val queue: List<QueueItem> = listOf(
        QueueItem("1", TrackSource.DEEZER, "One", "A", null, 200_000L),
        QueueItem("2", TrackSource.DEEZER, "Two", "B", null, 0L),
        QueueItem("content://media/external/audio/media/3", TrackSource.LOCAL, "Three", "C", null, 3_000L),
    )

    protected val otherQueue: List<QueueItem> = listOf(
        QueueItem("4", TrackSource.DEEZER, "Four", "D", null, 30_000L),
        QueueItem("5", TrackSource.DEEZER, "Five", "E", null, 30_000L),
    )

    @Test
    public fun `до подключения - Idle и нулевая позиция`() {
        assertEquals(PlaybackState.Idle, controller.state.value)
        assertEquals(0L, controller.positionMs.value)
    }

    @Test
    public fun `до подключения транспортные команды не меняют состояние и позицию`() {
        sendAllTransportCommands()

        assertEquals(PlaybackState.Idle, controller.state.value)
        assertEquals(0L, controller.positionMs.value)
    }

    @Test
    public fun `play с индексом вне очереди - IllegalArgumentException до и после подключения`() {
        assertInvalidPlayRejected()
        connect()
        assertInvalidPlayRejected()
    }

    @Test
    public fun `после подключения к пустой сессии - подключённый снапшот с пустой очередью`() {
        connect()

        val state = active()
        assertTrue(state.queue.isEmpty())
        assertTrue(state.isConnected)
        assertFalse(state.canSeek)
        assertFalse(state.canSeekBack)
        assertFalse(state.canSeekForward)
    }

    @Test
    public fun `подготовленный трек - снапшот разрешает перемотку`() {
        connect()
        controller.play(queue, 0)
        awaitSeekable()

        val state = active()
        assertTrue(state.canSeek)
        assertTrue(state.canSeekBack)
        assertTrue(state.canSeekForward)
    }

    @Test
    public fun `play при подключении сразу публикует очередь и индекс, позиция 0`() {
        connect()

        controller.play(queue, 1)
        assertCurrent(queue, 1)
        assertEquals(0L, controller.positionMs.value)

        controller.seekTo(SEEK_POSITION_MS)
        controller.play(otherQueue, 0)
        assertCurrent(otherQueue, 0)
        assertEquals(0L, controller.positionMs.value)
    }

    @Test
    public fun `seekTo при подключении сразу обновляет позицию`() {
        connect()
        controller.play(queue, 0)
        awaitSeekable()

        controller.seekTo(SEEK_POSITION_MS)

        assertEquals(SEEK_POSITION_MS, controller.positionMs.value)
    }

    @Test
    public fun `повторный play той же очереди и трека не перезапускает трек`() {
        connect()
        controller.play(queue, 1)
        awaitSeekable()
        controller.seekTo(SEEK_POSITION_MS)

        controller.play(queue, 1)

        assertCurrent(queue, 1)
        assertEquals(SEEK_POSITION_MS, controller.positionMs.value)
    }

    @Test
    public fun `play до подключения исполняется после него, выигрывает последний`() {
        controller.play(queue, 0)
        controller.play(otherQueue, 1)
        assertEquals(PlaybackState.Idle, controller.state.value)

        connect()

        assertCurrent(otherQueue, 1)
    }

    @Test
    public fun `уход в фон - последний снапшот без подключения, команды его не меняют`() {
        connect()
        controller.play(queue, 2)
        awaitSeekable()
        controller.seekTo(SEEK_POSITION_MS)

        disconnect()
        sendAllTransportCommands()

        val state = active()
        assertEquals(queue, state.queue)
        assertEquals(2, state.currentIndex)
        assertFalse(state.isConnected)
        assertEquals(SEEK_POSITION_MS, controller.positionMs.value)
    }

    @Test
    public fun `play в фоне сразу обнуляет позицию, снапшот остаётся прежним до подключения`() {
        connect()
        controller.play(queue, 2)
        awaitSeekable()
        controller.seekTo(SEEK_POSITION_MS)
        disconnect()

        controller.play(otherQueue, 1)

        assertEquals(0L, controller.positionMs.value)
        val state = active()
        assertEquals(queue, state.queue)
        assertFalse(state.isConnected)

        connect()

        assertCurrent(otherQueue, 1)
    }

    @Test
    public fun `возврат из фона - снапшот снова подключён с прежней очередью`() {
        connect()
        controller.play(queue, 2)
        disconnect()

        connect()

        assertCurrent(queue, 2)
    }

    private fun sendAllTransportCommands() {
        controller.togglePlayPause()
        controller.seekTo(1_000L)
        controller.next()
        controller.previous()
        controller.seekBack()
        controller.seekForward()
    }

    private fun assertInvalidPlayRejected() {
        assertThrows(IllegalArgumentException::class.java) { controller.play(emptyList(), 0) }
        assertThrows(IllegalArgumentException::class.java) { controller.play(queue, -1) }
        assertThrows(IllegalArgumentException::class.java) { controller.play(queue, queue.size) }
    }

    private fun assertCurrent(expectedQueue: List<QueueItem>, expectedIndex: Int) {
        val state = active()
        assertEquals(expectedQueue, state.queue)
        assertEquals(expectedIndex, state.currentIndex)
        assertTrue(state.isConnected)
    }

    private fun active(): PlaybackState.Active {
        val state = controller.state.value
        assertTrue("ожидался снапшот Active, а state = $state", state is PlaybackState.Active)
        return state as PlaybackState.Active
    }
}

private const val SEEK_POSITION_MS = 3_000L
