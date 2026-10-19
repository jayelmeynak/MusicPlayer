plugins {
    alias(libs.plugins.musicplayer.feature.api)
    alias(libs.plugins.kotlin.serialization)
    // FakePlaybackController and FakeTrackUriResolver for tests of modules that use the player.
    `java-test-fixtures`
}

dependencies {
    api(project(":lib:navigation"))
    api(libs.kotlinx.coroutines.core)

    // Общий набор контрактных тестов PlaybackController — JUnit-тесты в testFixtures.
    testFixturesImplementation(libs.junit)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
