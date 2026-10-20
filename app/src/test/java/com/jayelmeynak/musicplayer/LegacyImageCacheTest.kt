package com.jayelmeynak.musicplayer

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Robolectric: настоящий `cacheDir` приложения. Приложение без Hilt — проверяется только функция. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LegacyImageCacheTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `каталог прежнего дискового кэша Coil удаляется со всем содержимым`() {
        val legacy = File(context.cacheDir, LEGACY_CACHE_DIRECTORY)
        File(legacy, "journal").apply { parentFile!!.mkdirs() }.writeText("journal")
        File(legacy, "nested/entry.0").apply { parentFile!!.mkdirs() }.writeText("bytes")

        assertTrue(context.deleteLegacyImageCache())

        assertFalse(legacy.exists())
    }

    @Test
    fun `каталога нет - ничего не удаляется, без ошибки`() {
        assertFalse(context.deleteLegacyImageCache())
    }

    @Test
    fun `кэш Coil 3 и другие файлы cacheDir остаются`() {
        val legacy = File(context.cacheDir, "$LEGACY_CACHE_DIRECTORY/journal")
            .apply { parentFile!!.mkdirs() }.apply { writeText("journal") }
        val current = File(context.cacheDir, "coil3_disk_cache/journal")
            .apply { parentFile!!.mkdirs() }.apply { writeText("journal") }
        val other = File(context.cacheDir, "other.tmp").apply { writeText("other") }

        assertTrue(context.deleteLegacyImageCache())

        assertFalse(legacy.parentFile!!.exists())
        assertTrue(current.exists())
        assertTrue(other.exists())
    }

    private companion object {
        const val LEGACY_CACHE_DIRECTORY = "image_cache"
    }
}
