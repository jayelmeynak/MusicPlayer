package com.jayelmeynak.player.data

import com.jayelmeynak.lib.network.data.RemoteTrackDataSource
import com.jayelmeynak.lib.network.data.dto.ResponseChart
import com.jayelmeynak.lib.network.data.dto.TrackDto
import com.jayelmeynak.lib.network.data.dto.Tracks
import com.jayelmeynak.util.result.DataError
import com.jayelmeynak.util.result.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric only for android.net.Uri inside Track.
@RunWith(RobolectricTestRunner::class)
class MusicRemoteRepositoryImplTest {

    private val source = FakeRemoteTrackDataSource()
    private val repository = MusicRemoteRepositoryImpl(source)

    private val fullTrack = TrackDto(id = 42L, title = "Title", preview = "https://preview")

    @Test
    fun `трек без preview - SERIALIZATION`() = runTest {
        source.track = Result.Success(fullTrack.copy(preview = null))

        assertEquals(Result.Error(DataError.Remote.SERIALIZATION), repository.getTrack("42"))
    }

    @Test
    fun `полный трек - Success`() = runTest {
        source.track = Result.Success(fullTrack)

        val track = (repository.getTrack("42") as Result.Success).data

        assertEquals(42L, track.id)
        assertEquals("Title", track.title)
        assertEquals("https://preview", track.preview)
    }

    @Test
    fun `ошибка источника трека проходит как есть`() = runTest {
        source.track = Result.Error(DataError.Remote.NOT_FOUND)

        assertEquals(Result.Error(DataError.Remote.NOT_FOUND), repository.getTrack("0"))
    }

    @Test
    fun `альбом отбрасывает битые записи и сохраняет порядок`() = runTest {
        source.album = Result.Success(
            ResponseChart(
                Tracks(listOf(fullTrack.copy(id = 1L), fullTrack.copy(title = null), null, fullTrack.copy(id = 2L)))
            )
        )

        val ids = (repository.getAlbum("7") as Result.Success).data.map { it.id }

        assertEquals(listOf(1L, 2L), ids)
    }

    @Test
    fun `альбом без tracks - пустой список`() = runTest {
        source.album = Result.Success(ResponseChart())

        assertEquals(Result.Success(emptyList<Any>()), repository.getAlbum("7"))
    }

    private class FakeRemoteTrackDataSource : RemoteTrackDataSource {
        var track: Result<TrackDto, DataError.Remote> = Result.Error(DataError.Remote.UNKNOWN)
        var album: Result<ResponseChart, DataError.Remote> = Result.Success(ResponseChart())

        override suspend fun getTrack(id: String): Result<TrackDto, DataError.Remote> = track

        override suspend fun getAlbum(id: String): Result<ResponseChart, DataError.Remote> = album
    }
}
