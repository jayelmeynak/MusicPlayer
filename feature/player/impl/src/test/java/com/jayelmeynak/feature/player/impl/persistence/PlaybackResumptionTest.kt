package com.jayelmeynak.feature.player.impl.persistence

import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.key
import com.jayelmeynak.feature.player.impl.service.TRACK_URI_SCHEME
import com.jayelmeynak.feature.player.impl.service.toQueueItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric — ради `Uri` и `Bundle` в элементах сессии. */
@RunWith(RobolectricTestRunner::class)
class PlaybackResumptionTest {

    private val queue = listOf(
        QueueItem("1", TrackSource.DEEZER, "One", "A", "https://cdn.example/1.jpg", 30_000L),
        QueueItem("content://media/external/audio/media/2", TrackSource.LOCAL, "Two", "B", null, 2_000L),
        QueueItem("3", TrackSource.DEEZER, "Three", "C", null, 30_000L),
    )

    private val store = FakePlaybackStateStore(
        queue = SavedQueue(queue.map { it.toSaved() }),
        position = SavedPosition(index = 2, key = queue[2].key, positionMs = 42_000L),
    )

    @Test
    fun `для воспроизведения - вся очередь с внутренними URI, индекс и позиция`() = runTest {
        val result = PlaybackResumption(store).load(forPlayback = true)

        requireNotNull(result)
        assertEquals(queue, result.mediaItems.map { it.toQueueItem() })
        assertTrue(result.mediaItems.all { it.localConfiguration?.uri?.scheme == TRACK_URI_SCHEME })
        assertEquals(2, result.startIndex)
        assertEquals(42_000L, result.startPositionMs)
    }

    @Test
    fun `только метаданные - один текущий элемент`() = runTest {
        val result = PlaybackResumption(store).load(forPlayback = false)

        requireNotNull(result)
        assertEquals(listOf(queue[2]), result.mediaItems.map { it.toQueueItem() })
        assertEquals(0, result.startIndex)
        assertEquals(42_000L, result.startPositionMs)
    }

    @Test
    fun `нет сохранённой очереди - нечего возобновлять`() = runTest {
        assertNull(PlaybackResumption(FakePlaybackStateStore()).load(forPlayback = true))
    }
}
