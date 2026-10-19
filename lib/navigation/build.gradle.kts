plugins {
    alias(libs.plugins.musicplayer.lib.jvm)
    // FakeNavigator для тестов модулей с навигацией.
    `java-test-fixtures`
}

dependencies {
    api(libs.navigation3.runtime)
}
