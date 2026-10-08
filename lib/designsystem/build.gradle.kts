plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jayelmeynak.lib.designsystem"
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.core)

    api(project(":util:result"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
