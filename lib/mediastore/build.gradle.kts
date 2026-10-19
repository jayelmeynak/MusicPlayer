plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.musicplayer.hilt)
}

android {
    namespace = "com.jayelmeynak.lib.mediastore"
}

dependencies {
    // Чтение MediaStore синхронное (@WorkerThread): поток выбирает вызывающий, корутины не нужны.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
