package com.jayelmeynak.musicplayer.testing

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * Скриншот экрана при падении теста — снимок системы через `UiDevice` (не зависит от hardware-bitmap в
 * отрисовке приложения) в `kaspresso/screenshots/<класс>/<тест>.png` каталога [ArtifactsDirsProvider].
 * Ошибка самого снимка не подменяет ошибку теста.
 */
class FailureScreenshotRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description) {
        runCatching {
            val dir = ArtifactsDirsProvider().provideNew(File("screenshots/${description.className}"))
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                .takeScreenshot(File(dir, "${description.methodName}.png"))
        }
    }
}
