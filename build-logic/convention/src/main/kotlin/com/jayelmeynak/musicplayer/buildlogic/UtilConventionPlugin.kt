package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `util:<name>`: JVM, explicit API (общая настройка — [configureJvmModule]).
 */
class UtilConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configureJvmModule(explicitApi = true)
}
