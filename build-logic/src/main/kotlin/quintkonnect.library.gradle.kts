// Explicit API mode: https://kotlinlang.org/docs/whatsnew1420.html#stable-explicit-api-mode-for-library-authors
// ABI validation: https://kotlinlang.org/docs/gradle-binary-compatibility-validation.html
@file:OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)

plugins {
    id("quintkonnect.kotlin-jvm")
}

kotlin {
    explicitApi()
    abiValidation()
}
