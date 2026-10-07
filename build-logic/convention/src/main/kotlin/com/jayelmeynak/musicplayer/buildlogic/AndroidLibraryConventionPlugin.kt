package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Base Android library: SDK, JVM toolchain, build types. No explicit API —
 * used directly by the legacy `core:*` and `features:*` modules.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugin("android-library")
        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
        }
        configureKotlin(explicitApi = false)
    }
}
