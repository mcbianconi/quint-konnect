plugins {
    id("org.jetbrains.kotlin.jvm")
    id("quintkonnect.ktlint")
}

// io.github.mcbianconi is the verified namespace on central.sonatype.com (qk-j02b):
// https://central.sonatype.org/register/namespace/
group   = "io.github.mcbianconi"
version = "0.2.0"

kotlin {
    jvmToolchain(21)
}

// https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html#org.gradle.api.tasks.testing.Test:maxParallelForks
// Forks run their classes sequentially, so tests that set quintkonnect.* system properties stay
// safe; enabling in-JVM JUnit concurrency would not be.
tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
}
