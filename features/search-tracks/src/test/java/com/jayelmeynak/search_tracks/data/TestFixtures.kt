package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.network.data.dto.AlbumDto
import com.jayelmeynak.network.data.dto.Artist
import com.jayelmeynak.network.data.dto.TrackDto

internal fun albumDto() = AlbumDto(
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

internal fun trackDto() = TrackDto(
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
