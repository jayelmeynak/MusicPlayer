package com.jayelmeynak.feature.localmusic.impl.data

import app.cash.turbine.test
import com.jayelmeynak.feature.localmusic.impl.data.source.LocalTracksDataSource
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.data.MediaStoreAudio
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalTracksRepositoryImplTest {

    private val dataSource = FakeLocalTracksDataSource()
    private val repository = LocalTracksRepositoryImpl(dataSource)

    @Test
    fun `строка MediaStore - локальный трек с content URI строкой и длительностью в мс`() = runTest {
        dataSource.rows = listOf(
            MediaStoreAudio(
                id = 7,
                uri = "content://media/external/audio/media/7",
                title = "Numb",
                artist = "Linkin Park",
                durationMs = 185_000,
            )
        )

        assertEquals(
            Result.Success(
                listOf(
                    LocalTrack(
                        id = 7,
                        title = "Numb",
                        artistName = "Linkin Park",
                        duration = 185_000,
                        uri = "content://media/external/audio/media/7",
                    )
                )
            ),
            repository.getTracksList(),
        )
    }

    @Test
    fun `пустая медиатека - успех с пустым списком`() = runTest {
        assertEquals(Result.Success(emptyList<LocalTrack>()), repository.getTracksList())
    }

    @Test
    fun `исключение чтения - локальная ошибка UNKNOWN`() = runTest {
        dataSource.error = SecurityException("no audio permission")

        assertEquals(Result.Error(DataError.Local.UNKNOWN), repository.getTracksList())
    }

    @Test
    fun `отмена не превращается в ошибку`() {
        dataSource.error = CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            runTest { repository.getTracksList() }
        }
    }

    @Test
    fun `изменения медиатеки - из источника`() = runTest {
        repository.libraryChanges().test {
            dataSource.changes.emit(Unit)
            awaitItem()
        }
    }

    private class FakeLocalTracksDataSource : LocalTracksDataSource {
        var rows: List<MediaStoreAudio> = emptyList()
        var error: Exception? = null

        override suspend fun getTracksList(): List<MediaStoreAudio> {
            error?.let { throw it }
            return rows
        }

        val changes = MutableSharedFlow<Unit>()

        override fun audioChanges(): Flow<Unit> = changes
    }
}
