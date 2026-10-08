plugins {
    alias(libs.plugins.musicplayer.feature.api)
    alias(libs.plugins.kotlin.serialization)
    // FakePlaybackController and FakeTrackUriResolver for tests of modules that use the player.
    `java-test-fixtures`
}

dependencies {
    api(project(":lib:navigation"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
