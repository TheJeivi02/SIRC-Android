plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:platform"))
    implementation(libs.kotlinx.coroutines.core)
    implementation("javax.inject:javax.inject:1")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
