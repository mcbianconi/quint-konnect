package io.github.mcbianconi.quintkonnect.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class QuintAction(val name: String = "")
