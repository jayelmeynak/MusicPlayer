package com.jayelmeynak.musicplayer.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCompose() {
    applyPlugin("kotlin-compose")
    extensions.configure<LibraryExtension> {
        buildFeatures.compose = true
    }
    dependencies {
        add("implementation", platform(library("androidx-compose-bom")))
        add("implementation", library("androidx-ui"))
    }
}

internal fun Project.configureHilt() {
    applyPlugin("hilt")
    applyPlugin("ksp")
    dependencies {
        add("implementation", library("hilt-android"))
        add("ksp", library("hilt-compiler"))
    }
}
