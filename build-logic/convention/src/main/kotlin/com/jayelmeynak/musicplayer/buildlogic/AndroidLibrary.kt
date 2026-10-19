package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * База Android-библиотеки: SDK, JVM toolchain, build types, проверка графа, анализ lint по одному;
 * без explicit API.
 * Отдельного id плагина нет — её вызывают `musicplayer.lib` и `musicplayer.feature.impl|ui|di`.
 */
internal fun Project.configureAndroidLibrary() {
    applyPlugin("android-library")
    extensions.configure<LibraryExtension> {
        configureAndroidCommon(this)
        defaultConfig.consumerProguardFiles("consumer-rules.pro")
    }
    configureKotlin(explicitApi = false)
    forbidForeignImpl()
    serializeLintAnalysis()
}
