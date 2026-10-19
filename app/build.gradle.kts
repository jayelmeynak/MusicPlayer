plugins {
    alias(libs.plugins.musicplayer.application)
    alias(libs.plugins.musicplayer.compose)
    alias(libs.plugins.musicplayer.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.jayelmeynak.musicplayer"

    defaultConfig {
        applicationId = "com.jayelmeynak.musicplayer"
        versionCode = 1
        versionName = "1.0"
        // Hilt в инструментальных тестах: runner подставляет HiltTestApplication.
        testInstrumentationRunner = "com.jayelmeynak.musicplayer.HiltTestRunner"
        // Таймаут на инструментальный тест: зависание (ожидание простоя UI, системы) — падение одного
        // теста с артефактами, а не весь таймаут CI job.
        testInstrumentationRunnerArguments["timeout_msec"] = "180000"
    }

    // Фейковый сервер Deezer — общий для Robolectric- и инструментальных UI-тестов.
    sourceSets {
        getByName("test").kotlin.directories += "src/sharedTest/java"
        getByName("androidTest").kotlin.directories += "src/sharedTest/java"
    }

    // Compose UI-тестам на Robolectric нужен объединённый манифест с тестовой ComponentActivity.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {

    // ChartDestination — корень стека вкладки «Remote»; экран и резолвер Deezer приходят через :di.
    implementation(project(":feature:catalog:api"))
    implementation(project(":feature:catalog:di"))
    // LocalMusicDestination — корень стека вкладки «Local»; экран и резолвер локальных треков — через :di.
    implementation(project(":feature:local-music:api"))
    implementation(project(":feature:local-music:di"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:ui"))
    implementation(project(":feature:player:di"))
    implementation(project(":lib:designsystem"))
    implementation(project(":lib:navigation"))
    implementation(project(":util:coroutines"))
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.material3.adaptive.navigation.suite)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // FakeNavigator для теста контракта EntryInstaller.
    testImplementation(testFixtures(project(":lib:navigation")))
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    // Тестовая ComponentActivity для всех вариантов юнит-тестов, не только debug.
    testImplementation(libs.androidx.ui.test.manifest)
    // UI-тесты оболочки на Robolectric: Hilt testing, фейковый Deezer, фейковый контроллер плеера.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.mockwebserver3)
    // DeezerBaseUrlModule — заменяется адресом фейкового сервера.
    testImplementation(project(":lib:network"))
    testImplementation(testFixtures(project(":feature:player:api")))
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kakao.compose)

    // Инструментальные UI-тесты: настоящий плеер, уведомление, системные точки входа.
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.mockwebserver3)
    androidTestImplementation(project(":lib:network"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kaspresso)
    androidTestImplementation(libs.kaspresso.compose.support)
    // Используются напрямую: UiDevice (пробуждение, поворот) и screen-классы Kakao Compose из sharedTest.
    androidTestImplementation(libs.androidx.test.uiautomator)
    // Свой MediaController теста на сессии сервиса: слушатель событий плеера.
    androidTestImplementation(libs.media3.session)
    androidTestImplementation(libs.kakao.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
}