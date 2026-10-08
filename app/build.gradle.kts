plugins {
    alias(libs.plugins.musicplayer.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.jayelmeynak.musicplayer"

    defaultConfig {
        applicationId = "com.jayelmeynak.musicplayer"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {

    implementation(project(":features:search-tracks"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:ui"))
    implementation(project(":feature:player:di"))
    implementation(project(":features:download-tracks"))
    implementation(project(":lib:designsystem"))
    // LocalTrack: the local tab hands its visible list to the player.
    implementation(project(":lib:mediastore"))
    implementation(project(":lib:navigation"))
    implementation(project(":util:coroutines"))
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.material3.adaptive.navigation.suite)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}