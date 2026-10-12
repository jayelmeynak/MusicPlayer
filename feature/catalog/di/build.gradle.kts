plugins {
    alias(libs.plugins.musicplayer.feature.di)
}

android {
    namespace = "com.jayelmeynak.feature.catalog.di"
}

dependencies {
    api(project(":feature:catalog:api"))
    implementation(project(":feature:catalog:impl"))
    // TrackUriResolver — контракт плеера, его реализация Deezer живёт в каталоге.
    implementation(project(":feature:player:api"))
}
