package com.jayelmeynak.feature.player.impl.resolver

import com.jayelmeynak.feature.player.api.TrackSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric: the id is parsed as an android.net.Uri. */
@RunWith(RobolectricTestRunner::class)
class LocalContentUriResolverTest {

    private val resolver = LocalContentUriResolver()

    @Test
    fun `резолвер обслуживает локальные треки`() {
        assertEquals(TrackSource.LOCAL, resolver.source)
    }

    @Test
    fun `id локального трека и есть его content URI`() = runTest {
        val id = "content://media/external/audio/media/42"

        assertEquals(id, resolver.resolve(id))
    }

    @Test
    fun `не MediaStore - null`() = runTest {
        assertNull(resolver.resolve(""))
        assertNull(resolver.resolve("content://com.example.provider/audio/1"))
        assertNull(resolver.resolve("file:///sdcard/Music/a.mp3"))
        assertNull(resolver.resolve("https://example.com/a.mp3"))
    }
}
