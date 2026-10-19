package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * База Android-библиотеки: SDK, JVM toolchain, build types; без explicit API. Модули напрямую
 * не применяют — только поверх неё `musicplayer.lib` и `musicplayer.feature.impl|ui|di`.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("android-library")
        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
        }
        configureKotlin(explicitApi = false)
        forbidForeignImpl()
    }
}
