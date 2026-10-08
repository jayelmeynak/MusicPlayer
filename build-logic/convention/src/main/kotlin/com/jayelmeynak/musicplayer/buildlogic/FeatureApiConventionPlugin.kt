package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:api`: JVM, explicit API.
 *
 * `testDebugUnitTest` runs the JVM `test` task, as in [UtilConventionPlugin].
 */
class FeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlin(explicitApi = true)
            tasks.register("testDebugUnitTest") { dependsOn("test") }
        }
    }
}
