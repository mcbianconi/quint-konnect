// A standalone build, laid out like a project applying quint-konnect from Maven Central (README.md's
// "Quick start"). Run it from the repository root with `./gradlew -p example build`.
//
// The `eachPlugin` mapping and includeBuild("..") below are the only difference: they stand in for
// the Maven Central coordinates, so the plugin, `quint-konnect-ksp` and `quint-konnect-core` come
// from this repository's sources instead of a published release.
// https://docs.gradle.org/current/userguide/composite_builds.html
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    // Not `pluginManagement { includeBuild("..") }`: a build included there too is registered as a
    // plugin build only, and the dependencySubstitution below is silently ignored. Resolving the
    // plugin id to its module instead lets the substitution below cover the plugin jar as well.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.mcbianconi.quint-konnect") {
                // The version is irrelevant: the substitution below replaces the module whatever it is.
                useModule("io.github.mcbianconi:quint-konnect-gradle-plugin:0.1.0")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    // Same Kotlin/KSP versions as the plugin under development was built against.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "quint-konnect-example"

includeBuild("..") {
    name = "quint-konnect"
    // Published artifactIds (`quint-konnect-ksp`, ...) don't match the project names (`:ksp`, ...)
    // Gradle substitutes by default: without these, it would quietly resolve 0.1.0 from Maven Central.
    dependencySubstitution {
        substitute(module("io.github.mcbianconi:quint-konnect-gradle-plugin")).using(project(":gradle-plugin"))
        substitute(module("io.github.mcbianconi:quint-konnect-ksp")).using(project(":ksp"))
        substitute(module("io.github.mcbianconi:quint-konnect-core")).using(project(":core"))
    }
}
