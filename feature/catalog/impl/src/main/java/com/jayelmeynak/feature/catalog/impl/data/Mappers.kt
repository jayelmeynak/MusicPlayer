package com.jayelmeynak.feature.catalog.impl.data

import com.jayelmeynak.feature.catalog.impl.domain.model.Track
import com.jayelmeynak.lib.network.data.dto.TrackDto
import java.net.URI

/** Трек без `id`, `title` или `preview` — `null`: его нельзя показать и сыграть; сам URL превью не сохраняется, длительность Deezer (секунды) — в мс. */
internal fun TrackDto.toTrack(): Track? {
    if (preview == null) return null
    return Track(
        id = id ?: return null,
        title = title ?: return null,
        artistName = artist?.name.orEmpty(),
        coverUrl = album?.cover.orEmpty(),
        duration = duration?.times(1000L) ?: 0L,
        link = link?.takeIf(::isDeezerWebLink),
    )
}

/**
 * Ссылку с сервера открывает внешнее приложение, поэтому пропускается только страница сайта Deezer:
 * `https` и хост `deezer.com` или его поддомен. Иные схемы (`http`, `file`, `intent`) и домены, а
 * также неразбираемая строка — отбрасываются.
 */
private fun isDeezerWebLink(link: String): Boolean {
    val uri = try {
        URI(link.trim())
    } catch (malformed: Exception) {
        return false
    }
    val host = uri.host?.lowercase() ?: return false
    return uri.scheme.equals(HTTPS, ignoreCase = true) &&
        (host == DEEZER_HOST || host.endsWith(".$DEEZER_HOST"))
}

private const val HTTPS = "https"
private const val DEEZER_HOST = "deezer.com"
