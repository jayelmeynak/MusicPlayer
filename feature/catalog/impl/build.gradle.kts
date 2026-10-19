plugins {
    alias(libs.plugins.musicplayer.feature.impl)
}

android {
    namespace = "com.jayelmeynak.feature.catalog.impl"

    // Compose-тесты на Robolectric: нужен смёрженный манифест с тестовой ComponentActivity.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":feature:catalog:api"))
    // Воспроизведение (PlaybackController, QueueItem, TrackUriResolver) и ключ экрана плеера.
    implementation(project(":feature:player:api"))
    implementation(project(":lib:designsystem"))
    implementation(project(":lib:network"))
    implementation(project(":util:result"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material3)
    // Иконка «Открыть в Deezer» (OpenInNew).
    implementation(libs.androidx.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(project(":util:testing"))
    testImplementation(testFixtures(project(":feature:player:api")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
