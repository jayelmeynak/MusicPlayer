plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.musicplayer.hilt)
}

android {
    namespace = "com.jayelmeynak.lib.network"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    api(project(":util:result"))
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver3)
}