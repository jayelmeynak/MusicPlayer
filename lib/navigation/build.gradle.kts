plugins {
    alias(libs.plugins.musicplayer.lib.jvm)
    // FakeNavigator for tests of modules that navigate.
    `java-test-fixtures`
}

dependencies {
    api(libs.navigation3.runtime)
}
