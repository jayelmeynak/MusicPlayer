plugins {
    alias(libs.plugins.musicplayer.feature.di)
}

android {
    namespace = "com.jayelmeynak.feature.player.di"
}

dependencies {
    api(project(":feature:player:api"))
    implementation(project(":feature:player:impl"))
}
