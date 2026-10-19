package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `musicplayer.compose`: Compose в Android-модуле (приложение или библиотека) — плагин компилятора,
 * `buildFeatures.compose`, BOM и `ui` в `implementation`. Применяется после базового плагина модуля
 * (`musicplayer.application`, `musicplayer.lib`), иначе конфигурация падает.
 *
 * BOM тестовых конфигураций (`testImplementation`, `androidTestImplementation`,
 * `testFixturesImplementation`) модуль объявляет сам вместе с тестовыми артефактами Compose.
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val android = requireAndroidExtension(pluginId = "musicplayer.compose")
        applyPlugin("kotlin-compose")
        android.buildFeatures.compose = true
        dependencies {
            add("implementation", platform(library("androidx-compose-bom")))
            add("implementation", library("androidx-ui"))
        }
    }
}
