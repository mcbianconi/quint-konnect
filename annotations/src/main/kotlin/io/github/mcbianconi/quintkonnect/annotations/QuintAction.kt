package io.github.mcbianconi.quintkonnect.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class QuintAction(public val name: String = "")
