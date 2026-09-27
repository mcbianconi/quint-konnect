package io.github.mcbianconi.quintkonnect.annotations

/**
 * A driver takes only one of `@QuintRun` and `@QuintTest`; KSP rejects a driver with both.
 *
 * @param ignore Spec state variables this driver doesn't model. `SpecTypesGenerator` (`ksp`)
 * leaves them out of the `<Module>Spec.<Driver>State` class it generates alongside the full
 * `<Module>Spec.State`, so `TypedState` can be built over that projection instead of a
 * hand-written class. A name here that isn't a state variable is a compile error. This is
 * unrelated to `@QuintIgnore`, which keeps a field (with a value) but excludes it from
 * comparison — `ignore` drops the field entirely. The nondet-tracking variable named by
 * `DriverConfig.nondetPath` (docs/decisions/quint-test-needs-nondet-path.md) is a common case:
 * it still needs listing here separately, since KSP can't see a driver's runtime `config()`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class QuintTest(
    public val spec: String,
    public val test: String,
    public val main: String = "",
    public val maxSamples: Int = -1,
    public val seed: String = "",
    public val ignore: Array<String> = [],
)
