package io.github.mcbianconi.quintkonnect.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class QuintTest(
    val spec: String,
    val test: String,
    val main: String = "",
    val maxSamples: Int = -1,
    val seed: String = "",
)
