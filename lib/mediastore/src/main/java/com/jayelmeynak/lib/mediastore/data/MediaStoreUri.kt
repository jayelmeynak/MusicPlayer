package com.jayelmeynak.lib.mediastore.data

import android.provider.MediaStore
import androidx.core.net.toUri

/** `content://media/...`: строка MediaStore. */
public fun isMediaStoreUri(id: String): Boolean {
    val uri = id.toUri()
    return uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY
}
