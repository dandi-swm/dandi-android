import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// 릴리즈 서명 정보. 공개 저장소라 소스에 두지 않고 git 무시 대상인 local.properties(없거나 빈 값이면 환경변수)에서 읽는다.
//   RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD
// 하나라도 없으면 release 는 서명하지 않는다(debug 키로 대신 서명해 잘못된 릴리즈가 나가지 않게 한다).
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(key: String): String? =
    localProperties.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(key)?.takeIf { it.isNotBlank() }

val releaseSigning = listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
    .associateWith(::signingValue)
    .takeIf { values -> values.values.all { it != null } }
    ?.mapValues { it.value!! }

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

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("RELEASE_STORE_FILE"))
                storePassword = releaseSigning.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("RELEASE_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
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
        // minSdk 24 에서 java.time 을 쓰기 위해 켠다.
        isCoreLibraryDesugaringEnabled = true
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
        // 비정상 종료 보고(Crashlytics)도 release 에서만 보낸다. 개발 중 크래시가 실사용자 지표에 섞이지 않게 한다.
        variant.manifestPlaceholders.put("firebaseCrashlyticsCollectionEnabled", isReleaseBuild.toString())
        // 평문 HTTP는 debug 에서만 허용한다(로컬 서버 등). 개발·운영 서버는 모두 HTTPS 라 그 밖의 빌드는 막는다.
        variant.manifestPlaceholders.put("usesCleartextTraffic", (variant.buildType == "debug").toString())
    }
}

// Crashlytics 난독화 매핑은 실제 release 빌드에서만 올린다. benchmark 와 베이스라인 프로파일용 빌드(benchmarkRelease 등)도
// 난독화되지만 측정용이라 매핑을 올릴 필요가 없고, 빌드할 때마다 네트워크 업로드가 붙지 않게 한다.
tasks.configureEach {
    if (name.startsWith("uploadCrashlyticsMappingFile") && name != "uploadCrashlyticsMappingFileRelease") {
        enabled = false
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

    implementation(project(":mailbox:presentation"))
    implementation(project(":mailbox:domain"))
    implementation(project(":mailbox:data"))
    implementation(project(":mailbox:entity"))

    implementation(project(":room:domain"))
    implementation(project(":room:data"))
    implementation(project(":room:entity"))

    implementation(project(":settings:presentation"))
    implementation(project(":settings:domain"))
    implementation(project(":settings:data"))
    implementation(project(":settings:entity"))

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
    implementation(project(":legal:presentation"))
    implementation(project(":legal:domain"))
    implementation(project(":legal:data"))
    implementation(project(":legal:entity"))

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

    // 서버 푸시(FCM) 수신
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // 비정상 종료 보고. 수집은 release 에서만 켠다(androidComponents 의 manifestPlaceholders).
    implementation(libs.firebase.crashlytics)

    implementation(libs.hilt.android)
    debugImplementation(libs.leakcanary)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.profileinstaller)

    "baselineProfile"(project(":baselineprofile"))

    testImplementation(libs.junit)
}