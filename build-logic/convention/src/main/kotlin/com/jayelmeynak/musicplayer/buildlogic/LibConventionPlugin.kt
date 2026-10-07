package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `lib:<name>`: Android library, explicit API.
 */
class LibConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("musicplayer-android-library")
        configureKotlin(explicitApi = true)
    }
}
