plugins {
    alias(libs.plugins.musicplayer.feature.api)
    alias(libs.plugins.kotlin.serialization)
    // FakePlaybackController и FakeTrackUriResolver для тестов модулей, работающих с плеером.
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
