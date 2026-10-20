package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Модуль `app`: Android-приложение (SDK, toolchain, release с R8 и сжатием ресурсов, проверка графа,
 * анализ lint по одному). Compose и Hilt подключаются плагинами `musicplayer.compose` и `musicplayer.hilt`.
 */
class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("android-application")
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig.targetSdk = MusicPlayerBuild.TARGET_SDK
            configureReleaseShrinking()
        }
        configureKotlin(explicitApi = false)
        forbidForeignImpl()
        serializeLintAnalysis()
    }
}
