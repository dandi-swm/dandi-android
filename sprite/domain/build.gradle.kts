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
    api(project(":sprite:entity"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
