package com.jayelmeynak.feature.player.impl.persistence

import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.key
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Сколько элементов очереди сохраняется: очередь — видимый список экрана, вплоть до всей локальной
 * медиатеки, а файл переписывается целиком при каждой смене очереди.
 */
internal const val MAX_SAVED_QUEUE_SIZE = 500

/** JSON файлов хранилища: незнакомые поля (из будущих версий) пропускаются. */
internal val savedPlaybackJson = Json { ignoreUnknownKeys = true }

/**
 * Сохранённая очередь сессии. Только то, что идентифицирует трек и что показывает плеер; ссылки на
 * поток здесь нет и быть не может — её даёт `TrackUriResolver` перед загрузкой.
 */
@Serializable
internal data class SavedQueue(val items: List<SavedQueueItem>)

/** Элемент сохранённой очереди; [source] — имя `TrackSource`. */
@Serializable
internal data class SavedQueueItem(
    val id: String,
    val source: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val durationMs: Long,
)

/**
 * Где остановилось воспроизведение: [key] (`QueueItem.key`) текущего элемента сверяется с очередью,
 * [index] различает одинаковые ключи; `-1` — индекс неизвестен.
 */
@Serializable
internal data class SavedPosition(val index: Int, val key: String, val positionMs: Long)

/**
 * Очередь для сохранения и индекс текущего элемента в ней; [playerIndices] — индекс каждого элемента
 * в плеере сессии: по нему находится играющий экземпляр, если трек в очереди повторяется.
 */
internal data class QueueWindow(val items: List<QueueItem>, val currentIndex: Int, val playerIndices: List<Int>)

/** С чего начать восстановленную сессию. */
internal data class RestorePoint(val items: List<QueueItem>, val index: Int, val positionMs: Long)

/**
 * Окно очереди сессии для сохранения: элементы без ключа трека (чужие) отбрасываются, из длинной
 * очереди остаётся не больше [limit] элементов вокруг текущего — `start = clamp(current − limit/2,
 * 0, size − limit)`. Текущий элемент всегда внутри окна, если он свой.
 */
internal fun queueWindow(
    queue: List<QueueItem?>,
    currentIndex: Int,
    limit: Int = MAX_SAVED_QUEUE_SIZE,
): QueueWindow {
    val own = queue.withIndex().filter { it.value != null }
    // Чужой текущий элемент: окно — вокруг ближайшего своего перед ним.
    val current = own.indexOfLast { it.index <= currentIndex }.coerceAtLeast(0)
    val items = own.map { requireNotNull(it.value) }
    val indices = own.map { it.index }
    if (items.size <= limit) return QueueWindow(items, current, indices)
    val start = (current - limit / 2).coerceIn(0, items.size - limit)
    return QueueWindow(
        items.subList(start, start + limit).toList(),
        current - start,
        indices.subList(start, start + limit).toList(),
    )
}

internal fun QueueItem.toSaved(): SavedQueueItem =
    SavedQueueItem(id, source.name, title, artist, artworkUri, durationMs)

/** Элемент очереди из сохранённого; `null` — источник, которого больше нет. */
internal fun SavedQueueItem.toQueueItem(): QueueItem? {
    val trackSource = TrackSource.entries.firstOrNull { it.name == source } ?: return null
    return QueueItem(id, trackSource, title, artist, artworkUri, durationMs)
}

/**
 * Точка восстановления: `null`, если очереди нет. Индекс берётся из [position], если ключ на нём
 * совпадает; иначе — первое вхождение ключа; иначе — первый элемент с нуля.
 */
internal fun restorePoint(queue: SavedQueue?, position: SavedPosition?): RestorePoint? {
    val items = queue?.items.orEmpty().mapNotNull { it.toQueueItem() }
    if (items.isEmpty()) return null
    if (position == null) return RestorePoint(items, 0, 0L)
    val index = when {
        items.getOrNull(position.index)?.key == position.key -> position.index
        else -> items.indexOfFirst { it.key == position.key }
    }
    if (index < 0) return RestorePoint(items, 0, 0L)
    return RestorePoint(items, index, position.positionMs.coerceAtLeast(0L))
}
