package com.jayelmeynak.feature.player.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStateTest {

    private val first = item("1")
    private val second = item("2")

    @Test
    fun `current is the queue item at current index`() {
        assertEquals(second, active(listOf(first, second), currentIndex = 1).current)
    }

    @Test
    fun `current is null when the queue is empty`() {
        assertNull(active(emptyList(), currentIndex = 0).current)
    }

    @Test
    fun `current is null when the index is outside the queue`() {
        assertNull(active(listOf(first), currentIndex = 1).current)
        assertNull(active(listOf(first), currentIndex = -1).current)
    }

    private fun active(queue: List<QueueItem>, currentIndex: Int) = PlaybackState.Active(
        queue = queue,
        currentIndex = currentIndex,
        isPlaying = false,
        playWhenReady = false,
        isBuffering = false,
        showPlayButton = true,
        durationMs = 0,
        error = null,
    )

    private fun item(id: String) = QueueItem(
        id = id,
        source = TrackSource.DEEZER,
        title = "Title $id",
        artist = "Artist",
        artworkUri = null,
        durationMs = 0,
    )
}
