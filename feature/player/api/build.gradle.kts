plugins {
    alias(libs.plugins.musicplayer.feature.api)
    alias(libs.plugins.kotlin.serialization)
    // MiniPlayerHost declares a @Composable function; the runtime comes with navigation3-runtime.
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    api(project(":lib:navigation"))
}
