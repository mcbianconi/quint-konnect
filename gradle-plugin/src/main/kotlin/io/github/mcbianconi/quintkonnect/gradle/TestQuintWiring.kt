package io.github.mcbianconi.quintkonnect.gradle

/**
 * Whether one `Test` task needs `quint`, and where that `quint` runs.
 *
 * [None] does not invoke `quint` (a replay, or a project that has no drivers and did not opt in).
 * [CheckOnly] invokes `quint` inside the test JVM, one driver at a time, so a `--tests` filter
 * never starts `quint` for a driver that does not run.
 * [Pregenerate] runs `generateQuintTraces` first and replays its files. Shrink never uses this:
 * shrinking needs a fresh `quint` run.
 */
internal enum class TestQuintWiring {
    None,
    CheckOnly,
    Pregenerate,
}

internal fun testQuintWiring(
    replay: Boolean,
    generateTraces: Boolean,
    hasDrivers: Boolean,
    shrink: Boolean,
): TestQuintWiring {
    if (replay) return TestQuintWiring.None
    if (shrink) return if (hasDrivers) TestQuintWiring.CheckOnly else TestQuintWiring.None
    if (generateTraces) return TestQuintWiring.Pregenerate
    if (hasDrivers) return TestQuintWiring.CheckOnly
    return TestQuintWiring.None
}
