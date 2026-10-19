package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Уровни SDK, test runner и build type release — общие для всех Android-модулей.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk = MusicPlayerBuild.COMPILE_SDK
        defaultConfig.apply {
            minSdk = MusicPlayerBuild.MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        buildTypes.getByName("release").apply {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

/**
 * JVM toolchain для Kotlin (Android или JVM) и, для целевых типов модулей, режим explicit API.
 */
internal fun Project.configureKotlin(explicitApi: Boolean) {
    extensions.getByType<KotlinProjectExtension>().apply {
        jvmToolchain(MusicPlayerBuild.JVM_TOOLCHAIN)
        if (explicitApi) explicitApi()
    }
}

/**
 * Android-расширение модуля для плагинов поверх базового (`musicplayer.compose`, `musicplayer.hilt`).
 * Без Android-плагина (JVM-модуль или неверный порядок в `plugins {}`) — понятная ошибка конфигурации,
 * а не молчаливый пропуск.
 */
internal fun Project.requireAndroidExtension(pluginId: String): CommonExtension =
    extensions.findByType<CommonExtension>() ?: throw GradleException(
        "$pluginId применяется к Android-модулю после его базового плагина " +
            "(musicplayer.application, musicplayer.lib, musicplayer.feature.impl|ui|di), а в $path Android-плагина нет."
    )
