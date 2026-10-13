plugins {
    alias(libs.plugins.musicplayer.feature.impl)
}

android {
    namespace = "com.jayelmeynak.feature.localmusic.impl"

    // Фейк репозитория треков — общий для тестов модуля.
    testFixtures {
        enable = true
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
    // Result и DataError.Local списка треков.
    implementation(project(":util:result"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    // Компилятор Compose применяется ко всем исходникам модуля, включая testFixtures: нужен Compose runtime,
    // отдельного алиаса для него в каталоге нет, ui тянет его транзитивно.
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.ui)
    testFixturesImplementation(project(":util:result"))

    testImplementation(libs.junit)
    testImplementation(testFixtures(project))
    testImplementation(project(":util:testing"))
    testImplementation(testFixtures(project(":feature:player:api")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
