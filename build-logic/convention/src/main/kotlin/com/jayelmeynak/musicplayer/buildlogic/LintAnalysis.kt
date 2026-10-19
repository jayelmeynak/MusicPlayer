package com.jayelmeynak.musicplayer.buildlogic

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Анализ lint (`lintAnalyze*`, `lintVitalAnalyze*`) во всей сборке идёт по одной задаче за раз.
 *
 * AGP запускает до трёх анализов одновременно (при куче демона 2 ГБ), и тогда K2 UAST lint изредка
 * падает на первом же файле анализа unit-тестов `app`: «Unexpected failure during lint analysis …:
 * Error while resolving FirRegularClassImpl from RAW_FIR». Сбой не зависит от исходников (sharedTest
 * в модели lint одинаков при `srcDir` и `directories`), от состояния демона (есть с `--no-daemon`),
 * от размера кучи и от того, в процессе Gradle lint или в отдельном воркере, — нужен только
 * одновременный запуск другого анализа lint. По одному анализу сбоя нет.
 */
internal fun Project.serializeLintAnalysis() {
    val slot = gradle.sharedServices.registerIfAbsent(
        "lintAnalysisSlot",
        LintAnalysisSlot::class.java,
    ) {
        maxParallelUsages.set(1)
    }
    tasks.named { LINT_ANALYSIS_TASK.matches(it) }.configureEach {
        usesService(slot)
    }
}

/** Единственный слот для задач анализа lint. */
internal abstract class LintAnalysisSlot : BuildService<BuildServiceParameters.None>

private val LINT_ANALYSIS_TASK = Regex("lint(Vital)?Analyze\\w+")
