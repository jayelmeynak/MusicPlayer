package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Room через KSP. Применяется поверх плагина Android-модуля (например, `musicplayer.lib`).
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("ksp")
        dependencies {
            add("implementation", library("room-runtime"))
            add("implementation", library("room-ktx"))
            add("ksp", library("room-compiler"))
        }
    }
}
