plugins {
    alias(libs.plugins.musicplayer.feature.ui)
}

android {
    namespace = "com.jayelmeynak.feature.player.ui"

    // Compose UI tests on Robolectric need the merged manifest with the test ComponentActivity.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Navigator comes with the player api.
    implementation(project(":feature:player:api"))
    implementation(project(":lib:designsystem"))

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)

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
