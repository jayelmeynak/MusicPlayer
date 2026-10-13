package com.jayelmeynak.feature.localmusic.impl.resolver

import android.Manifest
import com.jayelmeynak.feature.player.api.TrackSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Robolectric: id разбирается как android.net.Uri, разрешения — у тестового приложения (API 34). */
@RunWith(RobolectricTestRunner::class)
class LocalContentUriResolverTest {

    private val application = RuntimeEnvironment.getApplication()
    private val resolver = LocalContentUriResolver(application)

    @Before
    fun grantAudio() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
    }

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

    @Test
    fun `без разрешения на чтение аудио - null, трек недоступен`() = runTest {
        shadowOf(application).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)

        assertNull(resolver.resolve("content://media/external/audio/media/42"))
    }
}
