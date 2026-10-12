plugins {
    alias(libs.plugins.musicplayer.feature.impl)
}

android {
    namespace = "com.jayelmeynak.feature.localmusic.impl"

    // Тесты на Robolectric читают ресурсы модуля (строки экрана).
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":feature:local-music:api"))
    // Воспроизведение (PlaybackController, QueueItem, TrackUriResolver) и ключ экрана плеера.
    implementation(project(":feature:player:api"))
    implementation(project(":lib:designsystem"))
    // Чтение MediaStore и кэш обложек.
    implementation(project(":lib:mediastore"))
    implementation(project(":util:coroutines"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    testImplementation(project(":util:testing"))
    testImplementation(testFixtures(project(":feature:player:api")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
