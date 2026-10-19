package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `lib:<name>`: Android-библиотека, explicit API.
 */
class LibConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        configureAndroidLibrary()
        configureKotlin(explicitApi = true)
    }
}
