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
        // 카카오 SDK 는 카카오 자체 저장소에만 배포된다. 다른 그룹은 이 저장소에서 찾지 않는다.
        exclusiveContent {
            forRepository { maven("https://devrepo.kakao.com/nexus/content/groups/public/") }
            filter { includeGroup("com.kakao.sdk") }
        }
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

include(":home:presentation")
include(":home:domain")
include(":home:data")
include(":home:entity")

include(":meal:presentation")
include(":meal:domain")
include(":meal:data")
include(":meal:entity")

include(":history:presentation")
include(":history:domain")
include(":history:data")
include(":history:entity")

include(":collection:presentation")
include(":collection:domain")
include(":collection:data")
include(":collection:entity")

include(":shop:presentation")
include(":shop:domain")
include(":shop:data")
include(":shop:entity")

include(":tti")

include(":baselineprofile")

include(":auth:domain")
include(":auth:data")
include(":auth:presentation")
include(":auth:entity")

include(":intro:presentation")
include(":intro:domain")
include(":intro:data")
include(":intro:entity")

include(":onboarding:domain")
include(":onboarding:data")
include(":onboarding:entity")
