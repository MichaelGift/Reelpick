import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The recommendation engine shared by Reelpick and Playpick: plain Kotlin, no Android.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

dependencies {
    testImplementation(libs.junit)
}
