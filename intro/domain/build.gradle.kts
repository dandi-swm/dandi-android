plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

dependencies {
    api(project(":common:domain"))
    api(project(":intro:entity"))
    // 시작 분기 대상 Page 참조 (cross-feature Page 는 implementation)
    implementation(project(":home:domain"))
    implementation(project(":auth:domain"))
    implementation(project(":onboarding:domain"))
    // 홈으로 갈 때 냐미 미리 받기
    implementation(project(":cat:domain"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
