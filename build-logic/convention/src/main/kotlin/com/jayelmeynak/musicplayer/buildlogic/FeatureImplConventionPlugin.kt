package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:impl`: Android library with Compose, Hilt, KSP, explicit API.
 */
class FeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("musicplayer-android-library")
        configureCompose()
        configureHilt()
        configureKotlin(explicitApi = true)
    }
}
