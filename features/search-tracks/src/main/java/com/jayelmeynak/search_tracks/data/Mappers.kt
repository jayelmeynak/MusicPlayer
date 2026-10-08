package com.jayelmeynak.search_tracks.data

import com.jayelmeynak.lib.network.data.dto.AlbumDto
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.search_tracks.domain.models.Album
import com.jayelmeynak.search_tracks.domain.models.Track


// Null when Deezer sent a track without id, title or preview: such a track can't be shown or played.
fun TrackDto.toTrack(): Track? {
    return Track(
        id = id ?: return null,
        title = title ?: return null,
        preview = preview ?: return null,
        artistName = artist?.name.orEmpty(),
        album = album?.toAlbum() ?: EMPTY_ALBUM,
        // Deezer reports seconds.
        duration = duration?.times(1000L) ?: 0L,
    )
}

fun AlbumDto.toAlbum() = Album(
    id = id ?: 0,
    title = title.orEmpty(),
    cover = cover.orEmpty(),
    trackList = trackList.orEmpty(),
    type = type.orEmpty()
)

private val EMPTY_ALBUM = Album(id = 0, title = "", cover = "", trackList = "", type = "")
