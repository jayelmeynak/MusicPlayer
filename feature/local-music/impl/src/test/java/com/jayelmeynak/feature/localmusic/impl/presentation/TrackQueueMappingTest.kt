package com.jayelmeynak.feature.localmusic.impl.presentation

import com.jayelmeynak.feature.player.api.QueueItem
import com.jayelmeynak.feature.player.api.TrackSource
import com.jayelmeynak.feature.localmusic.impl.domain.model.LocalTrack
import org.junit.Assert.assertEquals
import org.junit.Test

/** Маппинг локального трека в элемент очереди. */
class TrackQueueMappingTest {

    @Test
    fun `локальный трек - id и обложка равны content-URI трека, длина в мс`() {
        val uri = "content://media/external/audio/media/7"
        val track = LocalTrack(id = 7L, title = "Title", artistName = "Artist", duration = 180_000, uri = uri)

        assertEquals(
            QueueItem(uri, TrackSource.LOCAL, "Title", "Artist", uri, 180_000L),
            track.toQueueItem(),
        )
    }
}
