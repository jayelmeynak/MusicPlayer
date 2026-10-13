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

/** Robolectric: настоящий каталог баз приложения. Приложение без Hilt — проверяется только функция. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LegacyArtworkCacheTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `файл прежнего кэша обложек удаляется вместе с журналом`() {
        val database = context.getDatabasePath(LEGACY_DATABASE_NAME)
        database.parentFile!!.mkdirs()
        database.writeText("legacy")
        val wal = File(database.path + "-wal").apply { writeText("wal") }

        assertTrue(context.deleteLegacyArtworkCache())

        assertFalse(database.exists())
        assertFalse(wal.exists())
    }

    @Test
    fun `файла нет - ничего не удаляется, без ошибки`() {
        assertFalse(context.deleteLegacyArtworkCache())
    }

    private companion object {
        const val LEGACY_DATABASE_NAME = "local_cache.db"
    }
}
