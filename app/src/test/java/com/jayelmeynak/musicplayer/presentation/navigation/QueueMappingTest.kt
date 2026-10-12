package com.jayelmeynak.musicplayer.presentation.navigation

import android.net.Uri
import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.lib.mediastore.domain.model.LocalTrack
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Маппинг локального трека; Deezer-трек маппит каталог. Robolectric — ради `Uri` в `LocalTrack`. */
@RunWith(RobolectricTestRunner::class)
class QueueMappingTest {

    @Test
    fun `локальный трек - id и обложка равны content-URI трека, длина в мс`() {
        val uri = "content://media/external/audio/media/7"
        val track = LocalTrack(id = 7L, title = "Title", artistName = "Artist", duration = 180_000, uri = Uri.parse(uri))

        assertEquals(
            QueueItem(uri, TrackSource.LOCAL, "Title", "Artist", uri, 180_000L),
            track.toQueueItem(),
        )
    }
}
