package com.jayelmeynak.feature.player.api

/**
 * Один трек очереди воспроизведения.
 *
 * Хранит то, что идентифицирует трек и что показывает плеер, но не URL потока: URL превью Deezer
 * истекает примерно через 15 минут, поэтому плеер берёт свежий у [TrackUriResolver] прямо перед
 * загрузкой трека.
 *
 * @property id Id трека в [source]; формат — см. [TrackSource].
 * @property artworkUri Обложка: адрес картинки или `content://` URI, обложка которого — эскиз его
 *   провайдера (`ContentResolver.loadThumbnail`); у локального трека — URI самого трека. `null` — нет обложки.
 * @property durationMs Полная длина трека, мс; `0`, если неизвестна.
 */
public data class QueueItem(
    val id: String,
    val source: TrackSource,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val durationMs: Long,
)

/**
 * Ключ элемента очереди `SOURCE|id`: один и тот же для `mediaId` сессии, ключа жеста перемотки и
 * сверки сохранённой позиции.
 */
public val QueueItem.key: String get() = "${source.name}|$id"
