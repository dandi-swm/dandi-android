import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dandi.nyummy.shop.presentation"
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
        // minSdk 24 에서 java.time 을 쓰기 위해 켠다.
        isCoreLibraryDesugaringEnabled = true
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
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
    implementation(project(":shop:domain"))
    implementation(project(":shop:entity"))
    // 구매 흐름의 "식사 기록하기"·"우편함 가기" 이동(홈과 같은 방식).
    implementation(project(":meal:domain"))
    implementation(project(":mailbox:domain"))
    // 코인 충전 받은 뒤 화면의 "홈으로"
    implementation(project(":home:domain"))
    // 장바구니 놓아보기 칸 모델. 놓아보기 화면 자체는 main이 슬롯으로 꽂는다.
    implementation(project(":room:entity"))
    implementation(project(":common:presentation"))

    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
