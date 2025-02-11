package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.network.data.dto.AlbumDto
import com.jayelmeynak.network.data.dto.TrackDto
import com.jayelmeynak.search_tracks.domain.models.Album
import com.jayelmeynak.search_tracks.domain.models.Track


fun TrackDto.toTrack() = Track(
    id = id,
    title = title,
    preview = preview,
    artistName = artist.name,
    album = album.toAlbum()
)

fun AlbumDto.toAlbum() = Album(
    id = id,
    title = title,
    cover = cover,
    trackList = trackList,
    type = type
)