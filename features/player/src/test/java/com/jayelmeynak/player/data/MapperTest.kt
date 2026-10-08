package com.jayelmeynak.player.data

import android.net.Uri
import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.lib.network.data.dto.AlbumDto
import com.jayelmeynak.lib.network.data.dto.Artist
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.player.domain.models.Album
import com.jayelmeynak.player.domain.models.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric only for android.net.Uri inside LocalTrack and Track.
@RunWith(RobolectricTestRunner::class)
class MapperTest {

    @Test
    fun `AlbumDto переводится в Album с cover`() {
        assertEquals(
            Album(id = 7, title = "Album", cover = "cover", trackList = "tracklist", type = "album"),
            albumDto().toAlbum(),
        )
    }

    @Test
    fun `TrackDto переводится в Track без uri и с duration 30, а не из DTO`() {
        assertEquals(
            Track(
                id = 42L,
                title = "Title",
                album = albumDto().toAlbum(),
                artistName = "Artist",
                preview = "https://preview",
                duration = 30,
                uri = null,
            ),
            trackDto().toTrack(),
        )
    }

    @Test
    fun `LocalTrack переводится в Track с uri в preview и без альбома`() {
        val uri = Uri.parse("content://media/external/audio/media/5")
        val localTrack = LocalTrack(id = 5, title = "Local", artistName = "Me", duration = 215, uri = uri)

        assertEquals(
            Track(
                id = 5,
                title = "Local",
                album = null,
                artistName = "Me",
                preview = "content://media/external/audio/media/5",
                duration = 215,
                uri = uri,
            ),
            localTrack.toTrack(),
        )
    }

    @Test
    fun `TrackDto без id, title или preview отбрасывается`() {
        assertNull(trackDto().copy(id = null).toTrack())
        assertNull(trackDto().copy(title = null).toTrack())
        assertNull(trackDto().copy(preview = null).toTrack())
    }

    @Test
    fun `TrackDto без исполнителя и альбома - пустое имя и альбом null`() {
        val track = trackDto().copy(artist = null, album = null).toTrack()

        assertEquals("", track?.artistName)
        assertNull(track?.album)
    }

    @Test
    fun `TrackDto с альбомом без id - альбом null, лишний запрос альбома не уходит`() {
        assertNull(trackDto().copy(album = albumDto().copy(id = null)).toTrack()?.album)
    }

    @Test
    fun `AlbumDto без полей переводится в Album с пустыми значениями`() {
        assertEquals(Album(id = 0, title = "", cover = "", trackList = "", type = ""), AlbumDto().toAlbum())
    }

    private fun albumDto() = AlbumDto(
        id = 7,
        title = "Album",
        cover = "cover",
        coverBig = "cover_big",
        coverMedium = "cover_medium",
        coverXl = "cover_xl",
        coverSmall = "cover_small",
        trackList = "tracklist",
        type = "album",
    )

    private fun trackDto() = TrackDto(
        id = 42L,
        title = "Title",
        titleShort = "Short",
        album = albumDto(),
        artist = Artist(
            id = 1,
            link = "link",
            name = "Artist",
            picture = "picture",
            pictureBig = "picture_big",
            pictureMedium = "picture_medium",
            pictureSmall = "picture_small",
            pictureXl = "picture_xl",
            radio = false,
            trackList = "artist_tracklist",
            type = "artist",
        ),
        duration = 180,
        explicitContentCover = 0,
        explicitContentLyrics = 0,
        explicitLyrics = false,
        link = "link",
        preview = "https://preview",
    )
}
