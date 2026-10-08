package com.jayelmeynak.feature.player.impl.artwork

import android.net.Uri
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import com.jayelmeynak.lib.mediastore.domain.repository.LocalTracksRepository
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric: the id is parsed as an android.net.Uri. */
@RunWith(RobolectricTestRunner::class)
class MediaStoreArtworkSourceTest {

    private val repository = RecordingRepository()
    private val source = MediaStoreArtworkSource(GetTrackArtworkUseCase(repository))

    @Test
    fun `трек MediaStore - обложка из кэша по id строки`() = runTest {
        val artwork = source.artwork("content://media/external/audio/media/42")

        assertArrayEquals(byteArrayOf(42), artwork)
        assertTrue(repository.requested.single() == 42L)
    }

    @Test
    fun `не MediaStore - null, кэш не трогается`() = runTest {
        assertNull(source.artwork("content://com.example.provider/audio/1"))
        assertNull(source.artwork("file:///sdcard/Music/1"))
        assertNull(source.artwork("content://media/external/audio/media/abc"))
        assertTrue(repository.requested.isEmpty())
    }

    private class RecordingRepository : LocalTracksRepository {
        val requested = mutableListOf<Long>()
        override suspend fun getTracksList(): List<LocalTrack> = emptyList()
        override suspend fun getArtwork(trackId: Long, uri: Uri): ByteArray? {
            requested += trackId
            return byteArrayOf(trackId.toByte())
        }
        override suspend fun pruneArtworkCache(activeTrackIds: List<Long>) = Unit
    }
}
