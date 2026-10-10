package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:api`: JVM, explicit API (общая настройка — [configureJvmModule]).
 */
class FeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configureJvmModule(explicitApi = true)
}
