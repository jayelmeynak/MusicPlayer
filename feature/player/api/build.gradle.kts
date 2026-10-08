plugins {
    alias(libs.plugins.musicplayer.feature.api)
    alias(libs.plugins.kotlin.serialization)
    // MiniPlayerHost declares a @Composable function; the runtime comes with navigation3-runtime.
    alias(libs.plugins.kotlin.compose)
    // FakePlaybackController and FakeTrackUriResolver for tests of modules that use the player.
    `java-test-fixtures`
}

dependencies {
    api(project(":lib:navigation"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
