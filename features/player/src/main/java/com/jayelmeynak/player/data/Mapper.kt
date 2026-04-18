package com.jayelmeynak.player.data

import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.network.data.dto.AlbumDto
import com.jayelmeynak.network.data.dto.TrackDto
import com.jayelmeynak.player.domain.models.Album
import com.jayelmeynak.player.domain.models.Track

fun TrackDto.toTrack() = Track(
    id = id,
    title = title,
    preview = preview,
    artistName = artist.name,
    album = album.toAlbum(),
    uri = null
)

fun AlbumDto.toAlbum() = Album(
    id = id,
    title = title,
    cover = cover,
    trackList = trackList,
    type = type
)

fun LocalTrack.toTrack() = Track(
    preview = uri.toString(),
    title = title,
    id = id,
    artistName = artistName,
    duration = duration,
    uri = uri,
    album = null
)
