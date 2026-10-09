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
    implementation(project(":util:coroutines"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
