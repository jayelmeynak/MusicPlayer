package com.jayelmeynak.feature.player.impl.persistence

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.feature.player.impl.service.testSessionCallback
import com.jayelmeynak.feature.player.impl.service.toMediaItem
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLooper

/**
 * Robolectric — ради настоящего ExoPlayer (`TestExoPlayer`): сохранение слушает события плеера.
 * Время дебаунса и периодической записи — на тестовом планировщике.
 */
@RunWith(RobolectricTestRunner::class)
class PlaybackPersistenceTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val store = FakePlaybackStateStore()
    private val persistence = PlaybackPersistence(store, scope, dispatcher)
    private val player: ExoPlayer = TestExoPlayerBuilder(RuntimeEnvironment.getApplication())
        .setMediaSourceFactory(FakeMediaSourceFactory())
        .build()

    @After
    fun tearDown() {
        persistence.release()
        player.release()
    }

    @Test
    fun `серия смен очереди быстрее дебаунса - одна запись последней очереди`() {
        persistence.attach(player)
        repeat(10) { n -> player.setMediaItems(mediaItems(queue.drop(n % 3))) }
        ShadowLooper.idleMainLooper()

        dispatcher.scheduler.advanceTimeBy(QUEUE_SAVE_DEBOUNCE_MS - 1)
        dispatcher.scheduler.runCurrent()
        assertTrue(store.savedQueues.isEmpty())

        dispatcher.scheduler.advanceTimeBy(2)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, store.savedQueues.size)
        assertEquals(queue.drop(9 % 3).map { it.toSaved() }, store.savedQueues.single().items)
    }

    @Test
    fun `запись очереди сопровождается позицией текущего элемента`() {
        persistence.attach(player)
        player.setMediaItems(mediaItems(queue), 2, 1_500L)
        ShadowLooper.idleMainLooper()

        dispatcher.scheduler.advanceUntilIdle()

        val position = store.savedPositions.last()
        assertEquals(SavedPosition(2, queue[2].key, 1_500L), position)
    }

    @Test
    fun `пустая очередь сессии не затирает сохранённую`() {
        persistence.attach(player)
        player.setMediaItems(mediaItems(queue))
        ShadowLooper.idleMainLooper()
        dispatcher.scheduler.advanceUntilIdle()

        player.clearMediaItems()
        ShadowLooper.idleMainLooper()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, store.savedQueues.size)
    }

    @Test
    fun `пауза записывает позицию, во время игры - раз в период`() {
        persistence.attach(player)
        player.setMediaItems(mediaItems(queue))
        player.prepare()
        player.play()
        run(player).untilState(Player.STATE_READY)
        ShadowLooper.idleMainLooper()
        assertTrue(player.isPlaying)
        dispatcher.scheduler.advanceTimeBy(QUEUE_SAVE_DEBOUNCE_MS + 1)
        dispatcher.scheduler.runCurrent()
        val beforePeriod = store.savedPositions.size

        dispatcher.scheduler.advanceTimeBy(POSITION_SAVE_PERIOD_MS * 2 + 1)
        dispatcher.scheduler.runCurrent()
        assertEquals(beforePeriod + 2, store.savedPositions.size)

        player.pause()
        // Часы плеера идут сами: пока лупер крутится, плеер мог сменить трек — это тоже запись.
        ShadowLooper.idleMainLooper()
        dispatcher.scheduler.runCurrent()
        val afterPause = store.savedPositions.size
        assertTrue(afterPause > beforePeriod + 2)
        assertEquals(player.currentPosition, store.savedPositions.last().positionMs)

        // Периодическая запись на паузе остановлена: планировщик доходит до простоя, записей нет.
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(afterPause, store.savedPositions.size)
    }

    @Test
    fun `release записывает отложенную очередь и позицию и останавливает период`() {
        persistence.attach(player)
        player.setMediaItems(mediaItems(queue), 1, 700L)
        ShadowLooper.idleMainLooper()

        persistence.release()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(queue.map { it.toSaved() }, store.savedQueues.single().items)
        assertEquals(SavedPosition(1, queue[1].key, 700L), store.savedPositions.last())
    }

    @Test
    fun `подготовка треков (обновление источника) очередь не переписывает`() {
        persistence.attach(player)
        player.setMediaItems(mediaItems(queue))
        ShadowLooper.idleMainLooper()
        dispatcher.scheduler.advanceUntilIdle()

        player.prepare()
        run(player).untilState(Player.STATE_READY)
        ShadowLooper.idleMainLooper()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, store.savedQueues.size)
    }

    @Test
    fun `текущий трек ушёл за окно очереди - окно пересчитано, индекс позиции в нём`() {
        val long = List(MAX_SAVED_QUEUE_SIZE + 100) { QueueItem("$it", TrackSource.DEEZER, "T$it", "A", null, 30_000L) }
        persistence.attach(player)
        player.setMediaItems(mediaItems(long))
        ShadowLooper.idleMainLooper()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(long.take(MAX_SAVED_QUEUE_SIZE).map { it.toSaved() }, store.savedQueues.single().items)

        player.seekTo(MAX_SAVED_QUEUE_SIZE + 50, 0L)
        run(player).untilPendingCommandsAreFullyHandled()
        ShadowLooper.idleMainLooper()
        dispatcher.scheduler.advanceUntilIdle()

        val position = store.savedPositions.last()
        val saved = store.savedQueues.last().items
        assertEquals(long[MAX_SAVED_QUEUE_SIZE + 50].key, position.key)
        assertTrue("индекс вне окна: $position", position.index >= 0)
        assertEquals(long[MAX_SAVED_QUEUE_SIZE + 50].toSaved(), saved[position.index])
        assertEquals(restorePoint(store.savedQueues.last(), position)?.items?.get(position.index), long[MAX_SAVED_QUEUE_SIZE + 50])
    }

    @Test
    fun `трек дважды в очереди - сохраняется индекс играющего экземпляра`() {
        val withDuplicate = listOf(queue[0], queue[1], queue[0])
        persistence.attach(player)
        player.setMediaItems(mediaItems(withDuplicate), 2, 500L)
        ShadowLooper.idleMainLooper()

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SavedPosition(2, queue[0].key, 500L), store.savedPositions.last())
    }

    private fun mediaItems(items: List<QueueItem>) =
        testSessionCallback().prepareMediaItems(items.map { it.toMediaItem() })

    private val queue = listOf(
        QueueItem("1", TrackSource.DEEZER, "One", "A", "https://cdn.example/1.jpg", 30_000L),
        QueueItem("2", TrackSource.DEEZER, "Two", "B", null, 30_000L),
        QueueItem("content://media/external/audio/media/3", TrackSource.LOCAL, "Three", "C", null, 3_000L),
    )
}
