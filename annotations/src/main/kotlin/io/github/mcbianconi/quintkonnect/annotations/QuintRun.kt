package io.github.mcbianconi.quintkonnect.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class QuintRun(
    val spec: String,
    val main: String = "",
    val init: String = "",
    val step: String = "",
    val maxSamples: Int = -1,
    val maxSteps: Int = -1,
    val seed: String = "",
)
