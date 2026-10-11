package com.jayelmeynak.feature.player.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackProgressTest {

    @Test
    fun `процент позиции от длительности`() {
        assertEquals(25f, progressPercent(positionMs = 1_000L, durationMs = 4_000L))
    }

    @Test
    fun `процент - 0 при неизвестной длительности`() {
        assertEquals(0f, progressPercent(positionMs = 1_000L, durationMs = 0L))
        assertEquals(0f, progressPercent(positionMs = 1_000L, durationMs = -1L))
    }

    @Test
    fun `процент ограничен 0 и 100`() {
        assertEquals(100f, progressPercent(positionMs = 5_000L, durationMs = 4_000L))
        assertEquals(0f, progressPercent(positionMs = -10L, durationMs = 4_000L))
    }

    @Test
    fun `позиция по проценту`() {
        assertEquals(1_000L, positionForPercent(percent = 25f, durationMs = 4_000L))
    }

    @Test
    fun `позиция по проценту - null при неизвестной длительности`() {
        assertNull(positionForPercent(percent = 50f, durationMs = 0L))
    }

    @Test
    fun `ключ элемента очереди - источник и id через черту`() {
        val item = QueueItem("42", TrackSource.DEEZER, "T", "A", null, 0L)

        assertEquals("DEEZER|42", item.key)
    }
}
