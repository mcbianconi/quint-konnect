// https://docs.gradle.org/current/userguide/plugins.html
plugins {
    alias(libs.plugins.kotlin.jvm)          apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp)                  apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
}
