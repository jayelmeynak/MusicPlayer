package com.jayelmeynak.feature.catalog.impl.domain.model

/**
 * Трек каталога Deezer. URL превью здесь нет: он живёт ~15 минут, играбельную ссылку получает
 * резолвер перед загрузкой трека.
 */
internal data class Track(
    val id: Long,
    val title: String,
    val artistName: String,
    /** Обложка альбома, пустая строка — обложки нет. */
    val coverUrl: String,
    /** Полная длина трека в мс, 0 — неизвестна; играется 30-секундное превью. */
    val duration: Long,
    /** Страница трека на Deezer, `null` — ссылки нет. */
    val link: String?,
)
