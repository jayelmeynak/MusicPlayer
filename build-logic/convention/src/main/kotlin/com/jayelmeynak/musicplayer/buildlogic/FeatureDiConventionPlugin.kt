package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:di`: Android-библиотека с Hilt (`musicplayer.hilt`), explicit API.
 */
class FeatureDiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        configureAndroidLibrary()
        applyPlugin("musicplayer-hilt")
        configureKotlin(explicitApi = true)
    }
}
