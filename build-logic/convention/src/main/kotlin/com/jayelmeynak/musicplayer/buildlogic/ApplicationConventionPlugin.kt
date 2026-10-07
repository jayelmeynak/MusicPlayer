package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The `app` module: Android application with Compose.
 */
class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("android-application")
        applyPlugin("kotlin-compose")
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig.targetSdk = MusicPlayerBuild.TARGET_SDK
            buildFeatures.compose = true
        }
        configureKotlin(explicitApi = false)
    }
}
