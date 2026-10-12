plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jayelmeynak.lib.designsystem"

    // Compose UI tests on Robolectric need the merged manifest with the test ComponentActivity.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    // Flow — в публичном API (debounceSearchQuery).
    api(libs.kotlinx.coroutines.core)

    api(project(":util:result"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    // Тестовая ComponentActivity для Robolectric-тестов SeekSlider: AGP подключает её через
    // debug-манифест модуля. Побочный эффект — она попадает в debug-сборку потребителей.
    debugImplementation(libs.androidx.ui.test.manifest)
}
