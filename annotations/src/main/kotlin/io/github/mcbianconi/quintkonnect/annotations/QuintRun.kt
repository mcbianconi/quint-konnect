package io.github.mcbianconi.quintkonnect.annotations

/**
 * @param ignore Spec state variables this driver doesn't model. `SpecTypesGenerator` (`ksp`)
 * leaves them out of the `<Module>Spec.<Driver>State` class it generates alongside the full
 * `<Module>Spec.State`, so `TypedState` can be built over that projection instead of a
 * hand-written class. A name here that isn't a state variable is a compile error. This is
 * unrelated to `@QuintIgnore`, which keeps a field (with a value) but excludes it from
 * comparison — `ignore` drops the field entirely.
 */
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
    public val invariants: Array<String> = [],
    public val ignore: Array<String> = [],
)
