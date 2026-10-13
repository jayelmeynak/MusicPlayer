package com.jayelmeynak.lib.mediastore.data

/**
 * Строка аудио MediaStore. [uri] — `content://` URI строки, [artist] — пустой, если у файла нет тега
 * исполнителя, [durationMs] — длительность в миллисекундах.
 */
public data class MediaStoreAudio(
    public val id: Long,
    public val uri: String,
    public val title: String,
    public val artist: String,
    public val durationMs: Int,
)
