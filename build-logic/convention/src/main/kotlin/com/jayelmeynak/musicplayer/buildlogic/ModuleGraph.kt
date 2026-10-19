package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.withType

/**
 * Дешёвая проверка графа до тестов архитектуры: модуль `X:impl` подключается только из `X:di`
 * той же фичи. Любая другая проектная зависимость на `…:impl` (из `app`, `:ui`, чужого `:impl`)
 * роняет сборку на этапе конфигурации. Сам `X:di` подключает `X:impl` не через `api`-конфигурацию,
 * иначе классы `:impl` транзитивно утекут потребителям `:di`. Проверяются только прямые рёбра.
 */
internal fun Project.forbidForeignImpl() {
    val consumer = path
    configurations.configureEach {
        val configuration = name
        dependencies.withType<ProjectDependency>().configureEach {
            val target = path
            if (!target.endsWith(IMPL_SUFFIX) || target == consumer) return@configureEach
            val allowed = target.removeSuffix(IMPL_SUFFIX) + DI_SUFFIX
            if (consumer != allowed) {
                throw GradleException(
                    "$target подключается только из $allowed, а не из $consumer: " +
                        "остальные модули зависят от :api фичи."
                )
            }
            if (configuration.endsWith(API_CONFIGURATION_SUFFIX, ignoreCase = true)) {
                throw GradleException(
                    "$consumer подключает $target через $configuration: только implementation, " +
                        "иначе классы :impl утекут потребителям $consumer."
                )
            }
        }
    }
}

private const val IMPL_SUFFIX = ":impl"
private const val DI_SUFFIX = ":di"
/** Суффикс конфигурации, отдающей зависимость потребителям: `api`, `testFixturesApi`. */
private const val API_CONFIGURATION_SUFFIX = "Api"
