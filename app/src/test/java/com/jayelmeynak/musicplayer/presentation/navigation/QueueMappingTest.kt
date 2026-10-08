package com.jayelmeynak.musicplayer.presentation.navigation

import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.search_tracks.domain.models.Album
import com.jayelmeynak.search_tracks.domain.models.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueMappingTest {

    @Test
    fun `трек Deezer - id, метаданные, обложка альбома и полная длина, без URL превью`() {
        val track = Track(
            id = 42L,
            title = "Title",
            album = Album(id = 1, title = "Album", cover = "https://cover", trackList = "", type = ""),
            artistName = "Artist",
            preview = "https://preview?hdnea=exp=1",
            duration = 180_000L,
        )

        assertEquals(
            QueueItem("42", TrackSource.DEEZER, "Title", "Artist", "https://cover", 180_000L),
            track.toQueueItem(),
        )
    }

    @Test
    fun `альбом без обложки - обложки нет`() {
        val track = Track(
            id = 1L,
            title = "T",
            album = Album(id = 0, title = "", cover = "", trackList = "", type = ""),
            artistName = "A",
            preview = "p",
        )

        assertNull(track.toQueueItem().artworkUri)
    }
}
