package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Project

/**
 * Общий состав Android-модулей фичи с экранами (`:impl`, `:ui`): база Android-библиотеки,
 * `musicplayer.compose`, `musicplayer.hilt`, explicit API. Плагины `:impl` и `:ui` различаются
 * назначением модуля, а не составом сборки.
 */
internal fun Project.configureFeatureAndroidModule() {
    applyPlugin("musicplayer-android-library")
    applyPlugin("musicplayer-compose")
    applyPlugin("musicplayer-hilt")
    configureKotlin(explicitApi = true)
}
