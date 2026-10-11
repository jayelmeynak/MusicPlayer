package com.jayelmeynak.musicplayer.testing

import androidx.test.platform.app.InstrumentationRegistry
import com.kaspersky.kaspresso.files.dirs.DirsProvider
import java.io.File

/**
 * Артефакты Kaspresso (скриншоты, видео, дампы иерархии, logcat) — во внешний каталог приложения
 * `Android/data/<пакет>/files/kaspresso`: его забирает `adb pull` без AdbServer и без `run-as`.
 * БД и настройки приложения не трогаются; очищается только содержимое каталогов Kaspresso.
 */
class ArtifactsDirsProvider : DirsProvider {

    private val root: File by lazy {
        val external = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        File(checkNotNull(external) { "Внешнее хранилище недоступно" }, ROOT)
    }

    override fun provideNew(dest: File): File = root.resolve(dest).apply { mkdirs() }

    override fun provideCleared(dest: File): File = provideNew(dest).apply { listFiles()?.forEach { it.deleteRecursively() } }

    companion object {
        const val ROOT = "kaspresso"
    }
}
