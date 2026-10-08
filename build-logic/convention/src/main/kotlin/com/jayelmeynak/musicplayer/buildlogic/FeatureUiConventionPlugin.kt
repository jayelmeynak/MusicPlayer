package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:ui`: Android library with Compose, Hilt, KSP, explicit API.
 *
 * Hilt is here for `@HiltViewModel` view models of the module's public composables.
 */
class FeatureUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("musicplayer-android-library")
        configureCompose()
        configureHilt()
        configureKotlin(explicitApi = true)
    }
}
