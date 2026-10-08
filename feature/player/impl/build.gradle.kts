plugins {
    alias(libs.plugins.musicplayer.feature.impl)
}

android {
    namespace = "com.jayelmeynak.feature.player.impl"

    // Compose UI tests on Robolectric need the merged manifest with the test ComponentActivity.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":feature:player:api"))
    implementation(project(":lib:designsystem"))
    implementation(project(":lib:network"))
    implementation(project(":lib:mediastore"))
    implementation(project(":util:result"))
    implementation(project(":util:coroutines"))

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.media3.session)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.common)

    testImplementation(libs.junit)
    testImplementation(project(":util:testing"))
    testImplementation(testFixtures(project(":feature:player:api")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.media3.test.utils)
    testImplementation(libs.media3.test.utils.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
