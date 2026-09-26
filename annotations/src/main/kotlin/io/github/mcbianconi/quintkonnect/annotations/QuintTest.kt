package io.github.mcbianconi.quintkonnect.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class QuintTest(
    public val spec: String,
    public val test: String,
    public val main: String = "",
    public val maxSamples: Int = -1,
    public val seed: String = "",
)
