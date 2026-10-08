package com.jayelmeynak.feature.player.impl.service

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric: MediaItem and Uri are Android classes. The callback does not need a session for
 * adding items, so it is called directly.
 */
@RunWith(RobolectricTestRunner::class)
class PlaybackSessionCallbackTest {

    private val callback = PlaybackSessionCallback()

    @Test
    fun `элементы очереди получают внутренний URI источника и id, а не URL`() {
        val items = listOf(
            queueItem("42", TrackSource.DEEZER).toMediaItem(),
            queueItem(LOCAL_ID, TrackSource.LOCAL).toMediaItem(),
        )

        val added = callback.prepareMediaItems(items)

        assertEquals(TrackKey(TrackSource.DEEZER, "42"), added[0].localConfiguration?.uri?.toTrackKey())
        assertEquals(TrackKey(TrackSource.LOCAL, LOCAL_ID), added[1].localConfiguration?.uri?.toTrackKey())
        added.forEach { assertEquals(TRACK_URI_SCHEME, it.localConfiguration?.uri?.scheme) }
    }

    @Test
    fun `метаданные и mediaId сохраняются`() {
        val item = queueItem("42", TrackSource.DEEZER)

        val added = callback.prepareMediaItems(listOf(item.toMediaItem())).single()

        assertEquals(item, added.toQueueItem())
    }

    @Test
    fun `чужой элемент со своим URI остаётся как есть`() {
        val foreign = MediaItem.fromUri("https://example.com/1.mp3")

        assertSame(foreign, callback.prepareMediaItems(listOf(foreign)).single())
    }

    @Test
    fun `чужой элемент без URI получает внутренний URI, который не разрешится - ошибка плеера`() {
        val foreign = MediaItem.Builder()
            .setMediaId("42")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Foreign").build())
            .build()

        val added = callback.prepareMediaItems(listOf(foreign)).single()

        assertEquals(TRACK_URI_SCHEME, added.localConfiguration?.uri?.scheme)
        assertNull(added.localConfiguration?.uri?.toTrackKey())
        assertEquals(Uri.parse(added.localConfiguration?.uri.toString()), added.localConfiguration?.uri)
    }

    private fun queueItem(id: String, source: TrackSource) = QueueItem(
        id = id,
        source = source,
        title = "Title $id",
        artist = "Artist",
        artworkUri = null,
        durationMs = 1_000L,
    )

    private companion object {
        const val LOCAL_ID = "content://media/external/audio/media/7"
    }
}
