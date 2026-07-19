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
    api(project(":tti"))
    api(project(":common:entity"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
