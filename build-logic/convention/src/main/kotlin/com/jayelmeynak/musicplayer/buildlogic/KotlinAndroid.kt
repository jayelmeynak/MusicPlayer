package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * SDK levels, test runner and release build type shared by every Android module.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk = MusicPlayerBuild.COMPILE_SDK
        defaultConfig.apply {
            minSdk = MusicPlayerBuild.MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        buildTypes.getByName("release").apply {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

/**
 * JVM toolchain for Kotlin (Android or JVM) and, for target module types, explicit API mode.
 */
internal fun Project.configureKotlin(explicitApi: Boolean) {
    extensions.getByType<KotlinProjectExtension>().apply {
        jvmToolchain(MusicPlayerBuild.JVM_TOOLCHAIN)
        if (explicitApi) explicitApi()
    }
}
