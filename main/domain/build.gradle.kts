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
    implementation(project(":main:entity"))
    api(project(":common:domain"))
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
}
