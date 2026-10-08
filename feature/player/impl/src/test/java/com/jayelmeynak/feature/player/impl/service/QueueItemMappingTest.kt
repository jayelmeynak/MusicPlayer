package com.jayelmeynak.feature.player.impl.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric: MediaItem keeps metadata extras in a Bundle and the artwork as a Uri. */
@RunWith(RobolectricTestRunner::class)
class QueueItemMappingTest {

    @Test
    fun `трек Deezer переносится через MediaItem без потерь`() {
        val item = QueueItem(
            id = "3135556",
            source = TrackSource.DEEZER,
            title = "Harder, Better",
            artist = "Daft Punk",
            artworkUri = "https://e-cdns-images.dzcdn.net/images/cover/1.jpg",
            durationMs = 224_000L,
        )

        assertEquals(item, item.toMediaItem().toQueueItem())
    }

    @Test
    fun `локальный трек с content id и без обложки переносится без потерь`() {
        val item = QueueItem(
            id = "content://media/external/audio/media/42",
            source = TrackSource.LOCAL,
            title = "Local",
            artist = "Someone",
            artworkUri = null,
            durationMs = 0L,
        )

        assertEquals(item, item.toMediaItem().toQueueItem())
    }

    @Test
    fun `MediaItem не несёт URL, длительность полного трека лежит в extras`() {
        val mediaItem = deezerItem().toMediaItem()

        assertNull(mediaItem.localConfiguration)
        assertNull(mediaItem.mediaMetadata.durationMs)
        assertEquals(224_000L, mediaItem.mediaMetadata.extras?.getLong(EXTRA_TRACK_DURATION_MS))
        assertEquals("Harder, Better", mediaItem.mediaMetadata.title)
    }

    @Test
    fun `чужой MediaItem без источника и id не переводится в QueueItem`() {
        val foreign = MediaItem.Builder()
            .setMediaId("42")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Foreign").build())
            .build()

        assertNull(foreign.toQueueItem())
        assertNull(MediaItem.Builder().setMediaId("UNKNOWN|42").build().toQueueItem())
        assertNull(MediaItem.Builder().setMediaId("DEEZER|").build().toQueueItem())
    }

    @Test
    fun `внутренний URI трека переносит источник и id, включая content URI`() {
        TrackSource.entries.forEach { source ->
            val key = TrackKey(source, "content://media/external/audio/media/42?x=1|2")

            assertEquals(key, key.toTrackUri().toTrackKey())
        }
    }

    private fun deezerItem() = QueueItem(
        id = "3135556",
        source = TrackSource.DEEZER,
        title = "Harder, Better",
        artist = "Daft Punk",
        artworkUri = null,
        durationMs = 224_000L,
    )
}
