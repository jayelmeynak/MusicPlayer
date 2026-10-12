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
                link = "https://www.deezer.com/track/42",
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

    @Test
    fun `ссылка на страницу трека Deezer сохраняется`() {
        assertEquals("https://www.deezer.com/track/42", trackDto().toTrack()?.link)
        assertEquals("https://deezer.com/track/42", trackDto().copy(link = "https://deezer.com/track/42").toTrack()?.link)
    }

    @Test
    fun `ссылка не на сайт Deezer по https или пустая - null`() {
        listOf(
            "http://www.deezer.com/track/42",
            "file:///sdcard/track.mp3",
            "intent://deezer.com/track/42#Intent;scheme=https;end",
            "https://evil.com/track/42",
            "https://deezer.com.evil.com/track/42",
            "  ",
            "",
            "https://deezer com/track",
            null,
        ).forEach { link ->
            assertNull(link, trackDto().copy(link = link).toTrack()?.link)
        }
    }
}
