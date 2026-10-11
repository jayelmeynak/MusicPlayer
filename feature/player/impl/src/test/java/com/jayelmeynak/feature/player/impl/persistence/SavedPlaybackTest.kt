package com.jayelmeynak.feature.player.impl.persistence

import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.player.api.key
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedPlaybackTest {

    @Test
    fun `короткая очередь сохраняется целиком с тем же индексом`() {
        val queue = items(10)

        val window = queueWindow(queue, currentIndex = 7)

        assertEquals(queue, window.items)
        assertEquals(7, window.currentIndex)
    }

    @Test
    fun `длинная очередь - окно лимита вокруг текущего элемента`() {
        val queue = items(1_200)

        val window = queueWindow(queue, currentIndex = 600)

        assertEquals(MAX_SAVED_QUEUE_SIZE, window.items.size)
        assertEquals(queue[350], window.items.first())
        assertEquals(250, window.currentIndex)
        assertEquals(queue[600], window.items[window.currentIndex])
        assertEquals((350 until 850).toList(), window.playerIndices)
    }

    @Test
    fun `окно прижимается к краям очереди`() {
        val queue = items(1_200)

        val start = queueWindow(queue, currentIndex = 10)
        val end = queueWindow(queue, currentIndex = 1_190)

        assertEquals(queue[0], start.items.first())
        assertEquals(10, start.currentIndex)
        assertEquals(queue[700], end.items.first())
        assertEquals(queue[1_190], end.items[end.currentIndex])
    }

    @Test
    fun `чужие элементы отбрасываются, индекс указывает на тот же трек`() {
        val queue = listOf(item(0), null, item(2), null, item(4))

        val window = queueWindow(queue, currentIndex = 4)

        assertEquals(listOf(item(0), item(2), item(4)), window.items)
        assertEquals(2, window.currentIndex)
        assertEquals(listOf(0, 2, 4), window.playerIndices)
    }

    @Test
    fun `сохранённые элементы читаются обратно, неизвестный источник отбрасывается`() {
        val saved = SavedQueue(
            items = listOf(item(1).toSaved(), SavedQueueItem("x", "SPOTIFY", "T", "A", null, 0L), item(2).toSaved()),
        )

        val point = restorePoint(saved, SavedPosition(index = 1, key = item(2).key, positionMs = 42_000L))

        assertEquals(listOf(item(1), item(2)), point?.items)
        assertEquals(1, point?.index)
        assertEquals(42_000L, point?.positionMs)
    }

    @Test
    fun `индекс совпадает с ключом - позиция из сохранения`() {
        val saved = SavedQueue(items(5).map { it.toSaved() })

        val point = restorePoint(saved, SavedPosition(index = 3, key = item(3).key, positionMs = 7_000L))

        assertEquals(3, point?.index)
        assertEquals(7_000L, point?.positionMs)
    }

    @Test
    fun `индекс не совпадает, ключ найден в другом месте - индекс по ключу`() {
        val saved = SavedQueue(items(5).map { it.toSaved() })

        val point = restorePoint(saved, SavedPosition(index = 0, key = item(4).key, positionMs = 7_000L))

        assertEquals(4, point?.index)
        assertEquals(7_000L, point?.positionMs)
    }

    @Test
    fun `ключа нет в очереди - старт с первого элемента с нуля`() {
        val saved = SavedQueue(items(5).map { it.toSaved() })

        val point = restorePoint(saved, SavedPosition(index = 9, key = "DEEZER|missing", positionMs = 7_000L))

        assertEquals(0, point?.index)
        assertEquals(0L, point?.positionMs)
    }

    @Test
    fun `позиции нет - старт с первого элемента с нуля`() {
        val point = restorePoint(SavedQueue(items(2).map { it.toSaved() }), null)

        assertEquals(0, point?.index)
        assertEquals(0L, point?.positionMs)
    }

    @Test
    fun `пустая или отсутствующая очередь - восстанавливать нечего`() {
        assertNull(restorePoint(null, SavedPosition(0, item(0).key, 1L)))
        assertNull(restorePoint(SavedQueue(emptyList()), null))
    }

    @Test
    fun `в JSON элемента только id, источник и метаданные - поля для ссылки на поток нет`() {
        val json = savedPlaybackJson.encodeToString(SavedQueue.serializer(), SavedQueue(listOf(item(1).toSaved())))

        val element = savedPlaybackJson.parseToJsonElement(json).jsonObject
        val fields = (element.getValue("items") as JsonArray).single().jsonObject.keys

        assertEquals(setOf("id", "source", "title", "artist", "artworkUri", "durationMs"), fields)
        assertEquals(setOf("items"), (element as JsonObject).keys)
    }

    private fun items(count: Int) = List(count, ::item)

    private fun item(n: Int) = QueueItem(
        id = "$n",
        source = if (n % 2 == 0) TrackSource.DEEZER else TrackSource.LOCAL,
        title = "Title $n",
        artist = "Artist $n",
        artworkUri = if (n % 2 == 0) "https://cdn.example/$n.jpg" else null,
        durationMs = n * 1_000L,
    )
}
