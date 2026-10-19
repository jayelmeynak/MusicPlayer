package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:impl`: реализация фичи — экраны, `internal` Hilt-модули, use case, репозитории,
 * сервисы. Состав — [configureFeatureAndroidModule] (Android-библиотека, Compose, Hilt, explicit API).
 */
class FeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configureFeatureAndroidModule()
}
