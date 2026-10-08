plugins {
    alias(libs.plugins.musicplayer.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jayelmeynak.ui"
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    implementation(project(":util:result"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}