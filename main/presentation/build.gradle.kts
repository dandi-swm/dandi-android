import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.dandi.nyummy.main.presentation"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))
    if (providers.gradleProperty("composecompiler.reports").orNull == "true") {
        val outDir = rootProject.layout.buildDirectory.dir(
            "compose_reports/${project.path.replace(":", "_").trim('_')}"
        )
        reportsDestination.set(outDir)
        metricsDestination.set(outDir)
    }
}

dependencies {
    implementation(project(":main:domain"))
    implementation(project(":main:entity"))
    implementation(project(":common:presentation"))
    implementation(project(":history:domain"))
    implementation(project(":history:presentation"))
    implementation(project(":home:domain"))
    implementation(project(":home:presentation"))
    implementation(project(":mailbox:domain"))
    implementation(project(":mailbox:presentation"))
    implementation(project(":settings:domain"))
    implementation(project(":settings:presentation"))
    implementation(project(":meal:domain"))
    implementation(project(":meal:presentation"))
    implementation(project(":auth:domain"))
    implementation(project(":auth:presentation"))
    implementation(project(":intro:domain"))
    implementation(project(":intro:presentation"))
    implementation(project(":onboarding:domain"))
    implementation(project(":onboarding:presentation"))
    implementation(project(":collection:domain"))
    implementation(project(":collection:presentation"))
    implementation(project(":shop:domain"))
    implementation(project(":shop:presentation"))
    implementation(project(":quest:domain"))
    implementation(project(":quest:presentation"))
    implementation(project(":achievement:domain"))
    implementation(project(":achievement:presentation"))

    implementation(libs.androidx.activity.compose)
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
