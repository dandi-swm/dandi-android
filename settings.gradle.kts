pluginManagement {
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

rootProject.name = "Dandi"
include(":app")

include(":common:presentation")
include(":common:domain")
include(":common:data")
include(":common:entity")

include(":main:presentation")
include(":main:domain")
include(":main:data")
include(":main:entity")

include(":meal:domain")
include(":meal:data")
include(":meal:entity")

include(":tti")

include(":baselineprofile")
