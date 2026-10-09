package com.jayelmeynak.feature.player.impl.resolver

import android.provider.MediaStore
import androidx.core.net.toUri
import com.jayelmeynak.feature.player.api.TrackUriResolver
import javax.inject.Inject

/**
 * A local track's id is its MediaStore `content://` URI, playable as is; any other id (another
 * provider, `file://`, a URL) is refused. This is the second of two guards: the session never
 * plays the URI an item brings with it (only the track key in its `mediaId` reaches a resolver),
 * and this resolver opens only MediaStore rows. An adapter until the local music feature provides
 * its own resolver.
 */
internal class LocalContentUriResolver @Inject constructor() : TrackUriResolver {

    override suspend fun resolve(id: String): String? = id.takeIf(::isMediaStoreUri)
}

/** `content://media/...`: a row of MediaStore. */
internal fun isMediaStoreUri(id: String): Boolean {
    val uri = id.toUri()
    return uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY
}
