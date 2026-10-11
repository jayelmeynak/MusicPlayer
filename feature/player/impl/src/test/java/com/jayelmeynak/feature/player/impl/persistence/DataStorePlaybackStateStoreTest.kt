package com.jayelmeynak.feature.player.impl.persistence

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Настоящий DataStore над файлами во временном каталоге: формат и поведение при порче файла. */
class DataStorePlaybackStateStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private val queueFile get() = File(folder.root, "queue.json")
    private val positionFile get() = File(folder.root, "position.json")

    private fun store() = DataStorePlaybackStateStore(
        queueStore = savedQueueDataStore(scope) { queueFile },
        positionStore = savedPositionDataStore(scope) { positionFile },
    )

    private val queue = SavedQueue(
        listOf(
            SavedQueueItem("42", "DEEZER", "Title", "Artist", "https://cdn.example/42.jpg", 30_000L),
            SavedQueueItem("content://media/external/audio/media/7", "LOCAL", "Local", "Me", null, 1_000L),
        )
    )

    @Test
    fun `DataStore одного файла в кэше - один экземпляр, сколько бы компонентов его ни просили`() {
        val cache = DataStoreCache()
        val file = File(folder.root, "shared.json")

        val first = cache.get(file) { savedQueueDataStore(scope) { file } }
        val second = cache.get(file) { savedQueueDataStore(scope) { file } }

        assertSame(first, second)
    }

    @Test
    fun `файлов нет - восстанавливать нечего`() = runTest {
        val store = store()

        assertNull(store.readQueue())
        assertNull(store.readPosition())
    }

    @Test
    fun `очередь и позиция читаются тем же хранилищем после записи`() = runTest {
        val store = store()

        store.saveQueue(queue)
        store.savePosition(SavedPosition(index = 1, key = "LOCAL|content://media/external/audio/media/7", positionMs = 12_345L))

        assertEquals(queue, store.readQueue())
        assertEquals(SavedPosition(1, "LOCAL|content://media/external/audio/media/7", 12_345L), store.readPosition())
    }

    @Test
    fun `запись очереди не трогает файл позиции и наоборот`() = runTest {
        val store = store()

        store.saveQueue(queue)

        assertEquals(true, queueFile.exists())
        assertEquals(false, positionFile.exists())
    }

    @Test
    fun `пустая очередь читается как отсутствие очереди`() = runTest {
        val store = store()

        store.saveQueue(SavedQueue(emptyList()))

        assertNull(store.readQueue())
    }

    @Test
    fun `повреждённые файлы - восстанавливать нечего, следующая запись чинит`() = runTest {
        queueFile.writeText("{ это не json")
        positionFile.writeText("[]")
        val store = store()

        assertNull(store.readQueue())
        assertNull(store.readPosition())

        store.saveQueue(queue)
        assertEquals(queue, store.readQueue())
    }
}
