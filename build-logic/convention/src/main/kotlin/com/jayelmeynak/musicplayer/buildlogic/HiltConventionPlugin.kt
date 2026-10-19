package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `musicplayer.hilt`: Hilt через KSP в Android-модуле (приложение или библиотека) — плагины Hilt и
 * KSP, `hilt-android` и компилятор Hilt. Применяется после базового плагина модуля
 * (`musicplayer.application`, `musicplayer.lib`), иначе конфигурация падает.
 *
 * Тестовый Hilt (`hilt-android-testing`, компилятор для `kspTest`/`kspAndroidTest`) подключает
 * модуль с Hilt-тестами сам.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        requireAndroidExtension(pluginId = "musicplayer.hilt")
        applyPlugin("hilt")
        applyPlugin("ksp")
        dependencies {
            add("implementation", library("hilt-android"))
            add("ksp", library("hilt-compiler"))
        }
    }
}
