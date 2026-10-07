package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:di`: Android library with Hilt, explicit API.
 */
class FeatureDiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("musicplayer-android-library")
        configureHilt()
        configureKotlin(explicitApi = true)
    }
}
