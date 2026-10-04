import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// 공개 저장소라 키·자격 증명은 소스에 두지 않고 git 무시 대상인 local.properties 에서 주입한다.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun String.asJavaStringLiteral() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.dandi.nyummy.auth.presentation"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        // 카카오 로그인 네이티브 앱 키. local.properties(없거나 빈 값이면 환경변수)에서 주입한다.
        //   KAKAO_NATIVE_APP_KEY=...
        // 비어 있으면 SDK 초기화를 건너뛰고 카카오 버튼은 "준비 중" 안내만 띄운다(CI 등 키 없는 빌드).
        // 매니페스트의 카카오 리다이렉트 스킴(kakao{키})도 같은 값으로 채운다.
        val kakaoNativeAppKey = localProps.getProperty("KAKAO_NATIVE_APP_KEY")?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", kakaoNativeAppKey.asJavaStringLiteral())
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey

        // 테스트 계정 로그인 버튼용 자격 증명. 기본값(release 포함)은 빈 값이라 버튼이 노출되지 않는다.
        buildConfigField("String", "TEST_LOGIN_EMAIL", "\"\"")
        buildConfigField("String", "TEST_LOGIN_PASSWORD", "\"\"")
    }

    buildTypes {
        debug {
            // 공개 저장소·release APK 에 자격 증명이 남지 않도록 debug 빌드에만 local.properties 에서 주입한다.
            //   TEST_LOGIN_EMAIL=...
            //   TEST_LOGIN_PASSWORD=...
            buildConfigField(
                "String",
                "TEST_LOGIN_EMAIL",
                localProps.getProperty("TEST_LOGIN_EMAIL").orEmpty().asJavaStringLiteral(),
            )
            buildConfigField(
                "String",
                "TEST_LOGIN_PASSWORD",
                localProps.getProperty("TEST_LOGIN_PASSWORD").orEmpty().asJavaStringLiteral(),
            )
        }
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
    implementation(project(":auth:domain"))
    implementation(project(":auth:entity"))
    implementation(project(":common:presentation"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kakao.user)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
