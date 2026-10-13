plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.jayelmeynak.lib.mediastore"
}

dependencies {
    // Чтение MediaStore синхронное (@WorkerThread): поток выбирает вызывающий, корутины не нужны.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
