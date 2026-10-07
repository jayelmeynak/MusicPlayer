package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Room via KSP. Applied on top of an Android module plugin (e.g. `musicplayer.lib`).
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
