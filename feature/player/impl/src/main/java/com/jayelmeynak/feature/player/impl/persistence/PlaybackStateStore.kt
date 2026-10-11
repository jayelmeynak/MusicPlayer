package com.jayelmeynak.feature.player.impl.persistence

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** Имя файла очереди в каталоге DataStore приложения. */
internal const val SAVED_QUEUE_FILE = "playback_queue.json"

/** Имя файла позиции: пишется часто, поэтому отдельно от очереди. */
internal const val SAVED_POSITION_FILE = "playback_position.json"

/** Файл сохранённой очереди; приёмник медиакнопок смотрит только на его наличие. */
internal fun Context.savedQueueFile(): File = dataStoreFile(SAVED_QUEUE_FILE)

internal fun Context.savedPositionFile(): File = dataStoreFile(SAVED_POSITION_FILE)

/**
 * Есть ли что возобновлять — без чтения DataStore: файл очереди есть и длиннее пустой очереди.
 * Пустую (`{"items":[]}`) пишет обработчик порчи вместо повреждённого файла.
 *
 * Зовётся на главном потоке (приёмник медиакнопок): это один stat файла в каталоге приложения, без
 * открытия и чтения, — осознанно без `StrictMode.allowThreadDiskReads`, StrictMode в приложении не
 * включён. Содержимое не проверяется: повреждённый файл или очередь с неизвестными источниками
 * пройдут проверку, тогда сервис сам остановится после неудачного возобновления.
 */
internal fun Context.hasSavedQueue(): Boolean {
    val file = savedQueueFile()
    return file.isFile && file.length() > EMPTY_QUEUE_JSON_BYTES
}

/**
 * Последняя очередь сессии и место в ней — для возобновления после смерти процесса или остановки
 * сервиса. Чтение отдаёт `null`, если сохранять было нечего или файл повреждён.
 */
internal interface PlaybackStateStore {
    suspend fun readQueue(): SavedQueue?
    suspend fun readPosition(): SavedPosition?
    suspend fun saveQueue(queue: SavedQueue)
    suspend fun savePosition(position: SavedPosition)
}

/** Два файла DataStore: очередь переписывается редко, позиция — часто. */
internal class DataStorePlaybackStateStore @Inject constructor(
    private val queueStore: DataStore<SavedQueue>,
    private val positionStore: DataStore<SavedPosition>,
) : PlaybackStateStore {

    override suspend fun readQueue(): SavedQueue? =
        queueStore.data.first().takeIf { it.items.isNotEmpty() }

    override suspend fun readPosition(): SavedPosition? =
        positionStore.data.first().takeIf { it != NO_POSITION }

    override suspend fun saveQueue(queue: SavedQueue) {
        queueStore.updateData { queue }
    }

    override suspend fun savePosition(position: SavedPosition) {
        positionStore.updateData { position }
    }
}

private val NO_QUEUE = SavedQueue(emptyList())
private val NO_POSITION = SavedPosition(index = -1, key = "", positionMs = 0L)

private val EMPTY_QUEUE_JSON_BYTES =
    savedPlaybackJson.encodeToString(SavedQueue.serializer(), NO_QUEUE).encodeToByteArray().size

/**
 * DataStore по пути файла, один на файл в пределах кэша: DataStore запрещает два активных
 * экземпляра на один файл. Первый созданный экземпляр (и его скоуп) живёт, пока жив кэш.
 */
internal class DataStoreCache {

    private val stores = ConcurrentHashMap<String, DataStore<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(file: File, create: () -> DataStore<T>): DataStore<T> =
        stores.computeIfAbsent(file.absolutePath) { create() } as DataStore<T>
}

/**
 * Кэш процесса для провайдеров `PlayerImplModule`. Hilt-синглтона мало: `HiltTestApplication`
 * создаёт `SingletonComponent` на каждый инструментальный тест в том же процессе, и второй
 * DataStore на тот же файл уронил бы тест. Подменить провайдеры из тестов `app` нельзя — типы
 * хранилища `internal` в `:impl`. Юнит-тесты создают свой [DataStoreCache] и этот не трогают.
 */
internal val processDataStores = DataStoreCache()

/** DataStore очереди; [scope] задаёт поток файловых операций и время жизни хранилища. */
internal fun savedQueueDataStore(scope: CoroutineScope, produceFile: () -> File): DataStore<SavedQueue> =
    DataStoreFactory.create(
        serializer = JsonFileSerializer(NO_QUEUE, SavedQueue.serializer()),
        corruptionHandler = ReplaceFileCorruptionHandler { NO_QUEUE },
        scope = scope,
        produceFile = produceFile,
    )

internal fun savedPositionDataStore(scope: CoroutineScope, produceFile: () -> File): DataStore<SavedPosition> =
    DataStoreFactory.create(
        serializer = JsonFileSerializer(NO_POSITION, SavedPosition.serializer()),
        corruptionHandler = ReplaceFileCorruptionHandler { NO_POSITION },
        scope = scope,
        produceFile = produceFile,
    )

/** JSON-файл целиком; непарсящийся файл — [CorruptionException], его заменит обработчик порчи. */
private class JsonFileSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>,
) : Serializer<T> {

    override suspend fun readFrom(input: InputStream): T = try {
        savedPlaybackJson.decodeFromString(serializer, input.readBytes().decodeToString())
    } catch (e: IllegalArgumentException) {
        // В том числе SerializationException — его наследник.
        throw CorruptionException("Повреждён файл сохранённого воспроизведения", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(savedPlaybackJson.encodeToString(serializer, t).encodeToByteArray())
    }
}
