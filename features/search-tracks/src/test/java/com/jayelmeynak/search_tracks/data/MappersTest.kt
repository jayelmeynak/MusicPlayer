package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.search_tracks.domain.models.Album
import com.jayelmeynak.search_tracks.domain.models.Track
import org.junit.Assert.assertEquals
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
            ),
            track,
        )
    }
}
