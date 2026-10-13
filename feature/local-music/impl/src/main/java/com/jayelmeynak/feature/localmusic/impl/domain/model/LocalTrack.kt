package com.jayelmeynak.feature.localmusic.impl.domain.model

/** Аудиофайл устройства: [duration] — в миллисекундах, [uri] — `content://` URI строки MediaStore. */
internal data class LocalTrack(
    val id: Long,
    val title: String,
    val artistName: String,
    val duration: Int,
    val uri: String,
)
