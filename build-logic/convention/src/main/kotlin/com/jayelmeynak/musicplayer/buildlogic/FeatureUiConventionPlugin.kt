package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `feature:<domain>:ui`: публичные composable фичи для чужих модулей (например, мини-плеер для `app`)
 * и их `@HiltViewModel`; Hilt-модулей и привязок контрактов здесь нет — они в `:impl` и `:di`.
 * Состав — [configureFeatureAndroidModule]: KSP-компилятор Hilt нужен ради `@HiltViewModel`.
 */
class FeatureUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configureFeatureAndroidModule()
}
