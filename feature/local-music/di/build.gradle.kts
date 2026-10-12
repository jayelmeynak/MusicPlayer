plugins {
    alias(libs.plugins.musicplayer.feature.di)
}

android {
    namespace = "com.jayelmeynak.feature.localmusic.di"
}

dependencies {
    api(project(":feature:local-music:api"))
    implementation(project(":feature:local-music:impl"))
    // TrackUriResolver — контракт плеера, его реализация для локальных треков живёт в фиче.
    implementation(project(":feature:player:api"))
}
