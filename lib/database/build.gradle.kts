plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.musicplayer.room)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.jayelmeynak.lib.database"
}

dependencies {

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqlite.bundled.jvm)
}
