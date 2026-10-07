plugins {
    `kotlin-dsl`
}

group = "com.jayelmeynak.musicplayer.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("application") {
            id = libs.plugins.musicplayer.application.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.ApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.musicplayer.android.library.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("featureApi") {
            id = libs.plugins.musicplayer.feature.api.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.FeatureApiConventionPlugin"
        }
        register("featureImpl") {
            id = libs.plugins.musicplayer.feature.impl.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.FeatureImplConventionPlugin"
        }
        register("featureUi") {
            id = libs.plugins.musicplayer.feature.ui.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.FeatureUiConventionPlugin"
        }
        register("featureDi") {
            id = libs.plugins.musicplayer.feature.di.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.FeatureDiConventionPlugin"
        }
        register("lib") {
            id = libs.plugins.musicplayer.lib.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.LibConventionPlugin"
        }
        register("util") {
            id = libs.plugins.musicplayer.util.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.UtilConventionPlugin"
        }
        register("room") {
            id = libs.plugins.musicplayer.room.get().pluginId
            implementationClass = "com.jayelmeynak.musicplayer.buildlogic.RoomConventionPlugin"
        }
    }
}
