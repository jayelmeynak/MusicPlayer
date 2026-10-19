package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Project

/**
 * Общий состав Android-модулей фичи с экранами (`:impl`, `:ui`): база Android-библиотеки
 * ([configureAndroidLibrary]), `musicplayer.compose`, `musicplayer.hilt`, explicit API. Плагины
 * `:impl` и `:ui` различаются назначением модуля, а не составом сборки.
 */
internal fun Project.configureFeatureAndroidModule() {
    configureAndroidLibrary()
    applyPlugin("musicplayer-compose")
    applyPlugin("musicplayer-hilt")
    configureKotlin(explicitApi = true)
}
