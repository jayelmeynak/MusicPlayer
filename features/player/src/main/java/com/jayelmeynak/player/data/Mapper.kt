package com.jayelmeynak.player.data

import com.jayelmeynak.local.domain.model.LocalTrack
import com.jayelmeynak.lib.network.data.dto.AlbumDto
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.player.domain.models.Album
import com.jayelmeynak.player.domain.models.Track

// Null when Deezer sent a track without id, title or preview: such a track can't be played.
fun TrackDto.toTrack(): Track? {
    return Track(
        id = id ?: return null,
        title = title ?: return null,
        preview = preview ?: return null,
        artistName = artist?.name.orEmpty(),
        album = album?.toAlbum(),
        uri = null
    )
}

fun AlbumDto.toAlbum() = Album(
    id = id ?: 0,
    title = title.orEmpty(),
    cover = cover.orEmpty(),
    trackList = trackList.orEmpty(),
    type = type.orEmpty()
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
