import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// 기기 홈 화면 위젯은 Compose가 아니라 RemoteViews로 그린다. 문구 글꼴(까투리)을 레이아웃 XML로 지정하기 위해서다.
android {
    namespace = "com.dandi.nyummy.widget.presentation"
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
    implementation(project(":widget:domain"))
    implementation(project(":widget:entity"))
    implementation(project(":home:domain"))
    // 문구를 그릴 까투리·Pretendard 글꼴(res/font)과 글자 판정(AccentGlyphCoverage)을 쓴다.
    implementation(project(":common:presentation"))

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
