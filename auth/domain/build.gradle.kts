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
    api(project(":auth:entity"))
    // LoginUseCase 가 로그인 성공 시 HomePage 로 이동한다 (cross-feature Page 네비게이션)
    implementation(project(":home:domain"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
