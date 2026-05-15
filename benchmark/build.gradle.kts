plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

application {
    mainClass.set("io.github.aashutosh.mediapicker.benchmark.BenchmarkKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":media-picker"))
    implementation(libs.kotlinx.coroutines.core)
}
