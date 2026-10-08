plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.jayelmeynak.lib.mediastore"
}

dependencies {

    implementation(project(":lib:database"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
