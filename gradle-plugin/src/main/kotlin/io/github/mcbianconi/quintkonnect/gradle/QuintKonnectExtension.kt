package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

// CI's pin (.github/workflows/ci.yml), also the default `checkQuint` compares against.
public const val DEFAULT_QUINT_VERSION: String = "0.32.0"

public abstract class QuintKonnectExtension {
    public abstract val quintVersion: Property<String>

    // Opt-in: false keeps resolving "quint" from PATH (existing setups keep working unchanged).
    // true downloads quintVersion into the Gradle user home cache (DownloadQuintTask.kt) and
    // points Test tasks and checkQuint at it instead, so a fresh clone needs no Node/quint install.
    public abstract val downloadQuint: Property<Boolean>

    // Opt-out: true (the default) configures every Test task's testLogging with
    // exceptionFormat=FULL, showStandardStreams=true and events(FAILED), so a quint-konnect
    // failure's trace/step/action/diff message and its "Reproduce this error with" stderr line
    // both show up in the console instead of only in build/test-results/test/*.xml. Set to false
    // to configure testLogging yourself; a project's own testLogging configuration always wins
    // over the plugin's regardless of this flag, as long as it's applied after the plugin (e.g. in
    // the same build.gradle.kts, below the `plugins {}` block).
    public abstract val configureTestLogging: Property<Boolean>

    // Spec files `quintIr` runs `quint typecheck` on (QuintIrTask.kt), to expose action/nondet
    // names and types to KSP (qk-8i6m). Defaults to every .qnt file under src/test/resources;
    // call quintIrSpecs.setFrom(...) to replace the default entirely, or quintIrSpecs.from(...)
    // to add to it.
    public abstract val quintIrSpecs: ConfigurableFileCollection

    // Opt-in: true makes every Test task that is not replaying depend on `generateQuintTraces`
    // and replay its files. false (the default) leaves trace generation to test time, so a
    // `--tests` filter starts quint only for the drivers that run (qk-blt0).
    public abstract val generateTraces: Property<Boolean>

    // Opt-in: true makes every KSP task depend on `quintIr` and passes its output to the
    // processor, which then reports a @QuintAction name/parameter not found in the spec, a
    // parameter type that clearly doesn't match its nondet's, and a spec action reachable at
    // runtime with no @QuintAction covering it (qk-75ad), and generates `<Module>Spec` with
    // @Serializable mirrors of the spec's types and state (qk-ixox) -- but also means compiling
    // tests needs quint. false (the default) skips all of that, unchanged from before qk-75ad.
    public abstract val readSpecIr: Property<Boolean>
}
