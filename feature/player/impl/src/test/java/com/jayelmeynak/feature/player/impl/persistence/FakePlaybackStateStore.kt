package com.jayelmeynak.feature.player.impl.persistence

import kotlinx.coroutines.CompletableDeferred

/**
 * Хранилище в памяти: помнит последнее значение и все записи по порядку. [readGate] держит чтение
 * очереди до [openGate] — так тест ловит момент «восстановление ещё идёт».
 */
internal class FakePlaybackStateStore(
    var queue: SavedQueue? = null,
    var position: SavedPosition? = null,
) : PlaybackStateStore {

    val savedQueues = mutableListOf<SavedQueue>()
    val savedPositions = mutableListOf<SavedPosition>()

    var readGate: Boolean = false

    private val gate = CompletableDeferred<Unit>()

    fun openGate() {
        gate.complete(Unit)
    }

    override suspend fun readQueue(): SavedQueue? {
        if (readGate) gate.await()
        return queue?.takeIf { it.items.isNotEmpty() }
    }

    override suspend fun readPosition(): SavedPosition? = position

    override suspend fun saveQueue(queue: SavedQueue) {
        savedQueues += queue
        this.queue = queue
    }

    override suspend fun savePosition(position: SavedPosition) {
        savedPositions += position
        this.position = position
    }
}
