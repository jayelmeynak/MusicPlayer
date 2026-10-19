package com.jayelmeynak.feature.player.api

/** Откуда трек и как читать его id. */
public enum class TrackSource {
    /** Каталог Deezer; id — id трека в Deezer. */
    DEEZER,

    /** Аудио устройства из MediaStore; id — `content://` URI трека. */
    LOCAL,
}
