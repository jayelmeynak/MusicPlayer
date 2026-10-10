package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Общая настройка JVM-модуля (`util:*`, JVM-`lib:*`, `feature:*:api`): Kotlin JVM и Android Lint
 * по алиасам каталога, toolchain, explicit API, проверка графа.
 *
 * CI и гейт харнеса вызывают Android-имена задач `lintDebug` и `testDebugUnitTest`, поэтому они
 * есть и у JVM-модулей. `lintDebug` — алиас `lint` (поэтому `lintDebug --rerun` lint не перезапустит). `testDebugUnitTest` — отдельная `Test`-задача
 * над тестовым source set, а не алиас `test`: `--rerun` действует только на названную задачу, и
 * через алиас тесты JVM-модуля не перезапускались бы. `test` и `build` её не вызывают; если в
 * одной команде стоят и `build`/`test`, и `testDebugUnitTest`, тесты пройдут дважды.
 */
internal fun Project.configureJvmModule(explicitApi: Boolean) {
    applyPlugin("kotlin-jvm")
    applyPlugin("android-lint")
    configureKotlin(explicitApi)
    forbidForeignImpl()

    val testSourceSet = extensions.getByType<SourceSetContainer>().getByName("test")
    tasks.register<Test>("testDebugUnitTest") {
        description = "Запускает unit-тесты JVM-модуля под Android-именем задачи."
        group = "verification"
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnit()
    }
    tasks.register("lintDebug") {
        description = "Запускает lint JVM-модуля под Android-именем задачи."
        group = "verification"
        dependsOn("lint")
    }
}
