pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MusicPlayer"
include(":app")
include(":feature:player:api")
include(":feature:player:impl")
include(":feature:player:ui")
include(":feature:player:di")
include(":features:search-tracks")
include(":features:download-tracks")
include(":lib:designsystem")
include(":lib:network")
include(":lib:database")
include(":lib:mediastore")
include(":lib:navigation")
include(":util:result")
include(":util:coroutines")
include(":util:testing")
