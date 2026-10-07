package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:ui`: Android library with Compose, explicit API.
 */
class FeatureUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("musicplayer-android-library")
        configureCompose()
        configureKotlin(explicitApi = true)
    }
}
