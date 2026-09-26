plugins {
    id("org.jetbrains.kotlin.jvm")
}

// io.github.mcbianconi is the verified namespace on central.sonatype.com (qk-j02b):
// https://central.sonatype.org/register/namespace/
group   = "io.github.mcbianconi"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}
