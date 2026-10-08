plugins {
    alias(libs.plugins.musicplayer.util)
    // Only for the contract test: main never calls `entry<K>`, but EntryInstallerTest does, and this
    // inline function with @Composable content fails to compile without the Compose compiler.
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    api(libs.navigation3.runtime)
    testImplementation(libs.junit)
}
