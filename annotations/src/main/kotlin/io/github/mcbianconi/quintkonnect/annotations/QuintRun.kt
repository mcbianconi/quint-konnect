package io.github.mcbianconi.quintkonnect.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class QuintRun(
    public val spec: String,
    public val main: String = "",
    public val init: String = "",
    public val step: String = "",
    public val maxSamples: Int = -1,
    public val maxSteps: Int = -1,
    public val seed: String = "",
)
