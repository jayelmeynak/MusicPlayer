package com.jayelmeynak.feature.player.impl.resolver

import android.provider.MediaStore
import androidx.core.net.toUri
import com.jayelmeynak.feature.player.api.TrackUriResolver
import javax.inject.Inject

/**
 * A local track's id is its MediaStore `content://` URI, playable as is; anything else is refused,
 * so a foreign controller cannot make the player open an arbitrary provider or file. An adapter
 * until the local music feature provides its own resolver.
 */
internal class LocalContentUriResolver @Inject constructor() : TrackUriResolver {

    override suspend fun resolve(id: String): String? = id.takeIf(::isMediaStoreUri)
}

/** `content://media/...`: a row of MediaStore. */
internal fun isMediaStoreUri(id: String): Boolean {
    val uri = id.toUri()
    return uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY
}
