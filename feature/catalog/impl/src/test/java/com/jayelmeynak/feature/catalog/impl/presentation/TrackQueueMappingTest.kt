package com.jayelmeynak.feature.catalog.impl.presentation

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackQueueMappingTest {

    @Test
    fun `трек Deezer - id, метаданные, обложка альбома и полная длина, без URL превью`() {
        val track = Track(
            id = 42L,
            title = "Title",
            artistName = "Artist",
            coverUrl = "https://cover",
            duration = 180_000L,
            link = null,
        )

        assertEquals(
            QueueItem("42", TrackSource.DEEZER, "Title", "Artist", "https://cover", 180_000L),
            track.toQueueItem(),
        )
    }

    @Test
    fun `альбом без обложки - обложки нет`() {
        val track = Track(id = 1L, title = "T", artistName = "A", coverUrl = "", duration = 0L, link = null)

        assertNull(track.toQueueItem().artworkUri)
    }
}
