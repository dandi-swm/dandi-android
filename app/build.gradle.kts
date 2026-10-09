import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.dandi.nyummy"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dandi.nyummy"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            isProfileable = true
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

// 실사용자 성능 지표는 release 빌드에서만 Firebase 로 보낸다.
// benchmark 와 베이스라인 프로파일 플러그인이 만드는 빌드(nonMinifiedRelease, benchmarkRelease)는 release 설정을
// 이어받지만 측정용이라 빼야 하므로, 빌드 타입 이름으로 한 곳에서 정한다.
androidComponents {
    onVariants { variant ->
        val isReleaseBuild = variant.buildType == "release"
        variant.buildConfigFields?.put(
            "IS_RELEASE_BUILD",
            BuildConfigField("boolean", isReleaseBuild.toString(), "release 빌드에서만 성능 지표를 외부로 보낸다"),
        )
        // Firebase Performance SDK 수집도 release 에서만 켠다. 모으는 것은 SDK 자동 트레이스(앱 시작, 화면 렌더링)와
        // 이 앱의 커스텀 트레이스(TTI, 버벅임, 이미지 로딩)다. Performance Gradle 플러그인은 적용하지 않아 네트워크 요청은 자동 계측하지 않는다.
        variant.manifestPlaceholders.put("firebasePerformanceCollectionEnabled", isReleaseBuild.toString())
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
    implementation(project(":common:presentation"))
    implementation(project(":common:domain"))
    implementation(project(":common:data"))
    implementation(project(":common:entity"))

    implementation(project(":main:presentation"))
    implementation(project(":main:domain"))
    implementation(project(":main:data"))
    implementation(project(":main:entity"))

    implementation(project(":home:presentation"))
    implementation(project(":home:domain"))
    implementation(project(":home:data"))
    implementation(project(":home:entity"))

    implementation(project(":cat:domain"))
    implementation(project(":cat:data"))
    implementation(project(":cat:entity"))

    implementation(project(":meal:presentation"))
    implementation(project(":meal:domain"))
    implementation(project(":meal:data"))
    implementation(project(":meal:entity"))

    implementation(project(":history:presentation"))
    implementation(project(":history:domain"))
    implementation(project(":history:data"))
    implementation(project(":history:entity"))

    implementation(project(":collection:presentation"))
    implementation(project(":collection:domain"))
    implementation(project(":collection:data"))
    implementation(project(":collection:entity"))

    implementation(project(":shop:presentation"))
    implementation(project(":shop:domain"))
    implementation(project(":shop:data"))
    implementation(project(":shop:entity"))
    implementation(project(":quest:presentation"))
    implementation(project(":quest:domain"))
    implementation(project(":quest:data"))
    implementation(project(":quest:entity"))
    implementation(project(":achievement:presentation"))
    implementation(project(":achievement:domain"))
    implementation(project(":achievement:data"))
    implementation(project(":achievement:entity"))

    implementation(project(":auth:presentation"))
    implementation(project(":auth:domain"))
    implementation(project(":auth:data"))
    implementation(project(":auth:entity"))

    implementation(project(":intro:presentation"))
    implementation(project(":intro:domain"))
    implementation(project(":intro:data"))
    implementation(project(":intro:entity"))

    implementation(project(":onboarding:presentation"))
    implementation(project(":onboarding:domain"))
    implementation(project(":onboarding:data"))
    implementation(project(":onboarding:entity"))

    // FCM 리마인드 알림 + 냐미 홈 위젯 (임시 구현)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.hilt.android)
    debugImplementation(libs.leakcanary)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.profileinstaller)

    "baselineProfile"(project(":baselineprofile"))
}