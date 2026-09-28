// https://docs.gradle.org/current/userguide/plugins.html
plugins {
    alias(libs.plugins.kotlin.serialization) apply false
    id("quintkonnect.ktlint")
}

// build-logic/ can't apply its own convention plugins and example/ is laid out like a user project,
// so neither applies quintkonnect.ktlint; this project lints their files instead.
spotless {
    kotlin {
        target("build-logic/src/**/*.kt", "example/src/**/*.kt")
    }
    kotlinGradle {
        target("*.gradle.kts", "build-logic/*.gradle.kts", "build-logic/src/**/*.kts", "example/*.gradle.kts")
    }
}
