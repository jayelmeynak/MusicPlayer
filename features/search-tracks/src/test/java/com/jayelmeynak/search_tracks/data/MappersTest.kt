package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.search_tracks.domain.models.Album
import com.jayelmeynak.search_tracks.domain.models.Track
import com.jayelmeynak.lib.network.data.dto.AlbumDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    @Test
    fun `AlbumDto переводится в Album с cover, а не cover_big или cover_xl`() {
        val album = albumDto().toAlbum()

        assertEquals(
            Album(id = 7, title = "Album", cover = "cover", trackList = "tracklist", type = "album"),
            album,
        )
    }

    @Test
    fun `TrackDto переводится в Track с именем исполнителя и альбомом`() {
        val track = trackDto().toTrack()

        assertEquals(
            Track(
                id = 42L,
                title = "Title",
                album = albumDto().toAlbum(),
                artistName = "Artist",
                preview = "https://preview",
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
        assertEquals(Album(id = 0, title = "", cover = "", trackList = "", type = ""), track?.album)
    }

    @Test
    fun `альбом без полей переводится в Album с пустыми значениями`() {
        assertEquals(Album(id = 0, title = "", cover = "", trackList = "", type = ""), AlbumDto().toAlbum())
    }
}
