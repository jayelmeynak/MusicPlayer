package com.jayelmeynak.feature.catalog.impl.data

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    @Test
    fun `обложка трека - cover альбома, а не cover_big или cover_xl`() {
        assertEquals("cover", trackDto().toTrack()?.coverUrl)
    }

    @Test
    fun `TrackDto переводится в Track с именем исполнителя и обложкой альбома`() {
        val track = trackDto().toTrack()

        assertEquals(
            Track(
                id = 42L,
                title = "Title",
                artistName = "Artist",
                coverUrl = "cover",
                duration = 180_000L,
            ),
            track,
        )
    }

    @Test
    fun `длительность Deezer в секундах переводится в миллисекунды, без неё - 0`() {
        assertEquals(180_000L, trackDto().toTrack()?.duration)
        assertEquals(0L, trackDto().copy(duration = null).toTrack()?.duration)
    }

    @Test
    fun `трек без id, title или preview отбрасывается`() {
        assertNull(trackDto().copy(id = null).toTrack())
        assertNull(trackDto().copy(title = null).toTrack())
        assertNull(trackDto().copy(preview = null).toTrack())
    }

    @Test
    fun `трек без исполнителя и альбома получает пустые значения`() {
        val track = trackDto().copy(artist = null, album = null).toTrack()

        assertEquals("", track?.artistName)
        assertEquals("", track?.coverUrl)
    }

    @Test
    fun `альбом без обложки - пустая обложка`() {
        assertEquals("", trackDto().copy(album = albumDto().copy(cover = null)).toTrack()?.coverUrl)
    }
}
