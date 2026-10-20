package com.jayelmeynak.lib.designsystem.components

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Robolectric: нужен `Uri`. */
@RunWith(RobolectricTestRunner::class)
class TrackArtworkTest {

    @Test
    fun `content-обложка - эскиз провайдера`() {
        val uri = "content://media/external/audio/media/7"

        assertEquals(ContentThumbnail(Uri.parse(uri)), artworkRequestData(uri))
    }

    @Test
    fun `обложка по адресу уходит в Coil как есть`() {
        val url = "https://example.com/cover.jpg"

        assertEquals(url, artworkRequestData(url))
    }

    @Test
    fun `нет обложки - нет данных запроса`() {
        assertNull(artworkRequestData(null))
    }

    @Test
    fun `запрос content-обложки - свой загрузчик эскиза и ключ кэша по URI`() {
        val uri = "content://media/external/audio/media/7"

        val request = trackArtworkRequest(RuntimeEnvironment.getApplication(), uri)

        assertEquals("content-thumbnail:$uri", request.memoryCacheKey)
        assertNotNull(request.fetcherFactory)
        assertEquals(ContentThumbnail::class, request.fetcherFactory!!.second)
    }

    @Test
    fun `запрос обложки по адресу - стандартные загрузчик и ключ Coil`() {
        val request = trackArtworkRequest(RuntimeEnvironment.getApplication(), "https://example.com/cover.jpg")

        assertNull(request.memoryCacheKey)
        assertNull(request.fetcherFactory)
    }
}
