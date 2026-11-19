import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

/** local.properties 값, 없으면 같은 이름의 환경변수. 빈 값은 없는 것으로 본다. */
fun configValue(key: String): String? =
    (localProps.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }

val PlaceholderBaseUrl = "https://example.com/"

android {
    namespace = "com.dandi.nyummy.common.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        // API-CONFIG-INJECTION-POINT: API 키는 local.properties 에서 주입한다.
        //   API_KEY=...        (QA/Prod 빌드는 CI 단계에서 주입)
        buildConfigField("String", "API_KEY", "\"${localProps.getProperty("API_KEY").orEmpty()}\"")
    }

    // API-CONFIG-INJECTION-POINT: 베이스 URL 은 빌드 타입마다 다른 서버를 본다. local.properties, 없으면 환경변수에서 읽는다.
    //   API_BASE_URL_DEV=...   debug → 개발 서버 (예전 키 API_BASE_URL 도 받는다)
    //   API_BASE_URL_PROD=...  release → 운영 서버
    // 값이 없으면 빌드 가능한 placeholder 를 쓴다. release 가 개발 서버로 붙지 않도록 운영 값은 다른 키로 대신하지 않는다.
    buildTypes {
        debug {
            val apiBaseUrl = configValue("API_BASE_URL_DEV") ?: configValue("API_BASE_URL") ?: PlaceholderBaseUrl
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            val apiBaseUrl = configValue("API_BASE_URL_PROD") ?: PlaceholderBaseUrl
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
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
    implementation(project(":common:domain"))
    implementation(project(":common:entity"))

    // AndroidX core — PackageInfoCompat.getLongVersionCode (minSdk 24 대응)
    implementation(libs.androidx.core.ktx)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Local Storage
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // TTI 외부 전송(Firebase Performance 커스텀 트레이스)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.perf)

    // 푸시 토큰 등록, 삭제(PushTokenHelperImpl)
    implementation(libs.firebase.messaging)

    // 원격 구성(버전 확인, 기능 플래그). 인트로가 앱 시작 때 한 번 받아 둔다.
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
