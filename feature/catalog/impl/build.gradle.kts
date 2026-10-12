plugins {
    alias(libs.plugins.musicplayer.feature.impl)
}

android {
    namespace = "com.jayelmeynak.feature.catalog.impl"
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
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    testImplementation(project(":util:testing"))
    testImplementation(testFixtures(project(":feature:player:api")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
