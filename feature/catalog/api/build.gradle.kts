plugins {
    alias(libs.plugins.musicplayer.feature.api)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":lib:navigation"))
}
