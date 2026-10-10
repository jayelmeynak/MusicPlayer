plugins {
    alias(libs.plugins.musicplayer.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jayelmeynak.search_tracks"

    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(project(":lib:designsystem"))
    implementation(project(":lib:network"))
    implementation(project(":util:result"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    testImplementation(project(":util:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}