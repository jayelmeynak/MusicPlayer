plugins {
    alias(libs.plugins.musicplayer.feature.ui)
}

android {
    namespace = "com.jayelmeynak.feature.player.ui"

    // Compose UI-тестам на Robolectric нужен объединённый манифест с тестовой ComponentActivity.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Navigator приходит вместе с api плеера.
    implementation(project(":feature:player:api"))
    implementation(project(":lib:designsystem"))

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(project(":util:testing"))
    testImplementation(testFixtures(project(":feature:player:api")))
    testImplementation(testFixtures(project(":lib:navigation")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
