package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `util:<name>`: JVM, explicit API.
 *
 * `testDebugUnitTest` runs the JVM `test` task, so CI and the quality gate, which call
 * the Android task name, also cover JVM modules.
 */
class UtilConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlin(explicitApi = true)
            tasks.register("testDebugUnitTest") { dependsOn("test") }
        }
    }
}
