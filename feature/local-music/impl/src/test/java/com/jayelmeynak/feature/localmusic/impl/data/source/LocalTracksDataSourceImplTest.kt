package com.jayelmeynak.feature.localmusic.impl.data.source

import com.jayelmeynak.lib.mediastore.data.MediaStoreAudio
import com.jayelmeynak.lib.mediastore.data.MediaStoreAudioReader
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.Executors

class LocalTracksDataSourceImplTest {

    private val ioExecutor = Executors.newSingleThreadExecutor { Thread(it, IO_THREAD) }
    private val ioDispatcher = ioExecutor.asCoroutineDispatcher()
    private val reader = FakeMediaStoreAudioReader()
    private val dataSource = LocalTracksDataSourceImpl(reader, ioDispatcher)

    @After
    fun tearDown() {
        ioExecutor.shutdownNow()
    }

    @Test
    fun `строки MediaStore отдаются как есть`() = runTest {
        reader.rows = listOf(audio(1), audio(2))

        assertEquals(listOf(audio(1), audio(2)), dataSource.getTracksList())
    }

    @Test
    fun `MediaStore читается на внедрённом IO-диспетчере`() = runTest {
        dataSource.getTracksList()

        assertEquals(IO_THREAD, reader.readThread)
    }

    @Test
    fun `исключение чтения пробрасывается`() {
        reader.error = SecurityException("no audio permission")

        assertThrows(SecurityException::class.java) {
            runTest { dataSource.getTracksList() }
        }
    }

    private fun audio(id: Long) = MediaStoreAudio(
        id = id,
        uri = "content://media/external/audio/media/$id",
        title = "Title $id",
        artist = "Artist $id",
        durationMs = 1_000,
    )

    private class FakeMediaStoreAudioReader : MediaStoreAudioReader {
        var rows: List<MediaStoreAudio> = emptyList()
        var error: Exception? = null
        var readThread: String? = null

        override fun readAudio(): List<MediaStoreAudio> {
            readThread = Thread.currentThread().name
            error?.let { throw it }
            return rows
        }
    }

    private companion object {
        const val IO_THREAD = "io-test"
    }
}
