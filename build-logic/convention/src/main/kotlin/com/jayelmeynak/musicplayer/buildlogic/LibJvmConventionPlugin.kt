package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `lib:<name>` без Android API (например, `lib:navigation`, от которого зависят JVM-`:api`):
 * JVM, explicit API. Android-модули `lib:*` — [LibConventionPlugin].
 */
class LibJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configureJvmModule(explicitApi = true)
}
