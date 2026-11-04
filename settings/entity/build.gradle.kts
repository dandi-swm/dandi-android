plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

dependencies {
    api(project(":common:entity"))
    // 집사 성별(Gender)과 프로필 범위는 회원가입과 같은 것을 쓴다.
    api(project(":auth:entity"))
}
