plugins {
    alias(libs.plugins.musicplayer.lib)
    alias(libs.plugins.musicplayer.compose)
}

android {
    namespace = "com.jayelmeynak.lib.designsystem"

    // Compose UI-тестам на Robolectric нужен объединённый манифест с тестовой ComponentActivity.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {

    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    // Сеть Coil 3 — отдельный артефакт: без него обложки Deezer по URL не загрузятся.
    implementation(libs.coil.network.okhttp)
    // Flow — в публичном API (debounceSearchQuery).
    api(libs.kotlinx.coroutines.core)

    api(project(":util:result"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver3)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    // Тестовая ComponentActivity для Robolectric-тестов SeekSlider: AGP подключает её через
    // debug-манифест модуля. Побочный эффект — она попадает в debug-сборку потребителей.
    debugImplementation(libs.androidx.ui.test.manifest)
}
