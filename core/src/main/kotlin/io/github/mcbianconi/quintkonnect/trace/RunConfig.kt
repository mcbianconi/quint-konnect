package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path

// https://quint-lang.org/docs/cli#quint-run
public data class RunConfig(
    public val spec: String,
    public val main: String? = null,
    public val init: String? = null,
    public val step: String? = null,
    public val maxSamples: Int? = null,
    public val maxSteps: Int? = null,
    override val seed: String = genSeed(),
    override val invariants: List<String> = emptyList(),
) : GeneratorConfig {

    // Kept for binary compatibility with compiled callers of the pre-invariants constructor.
    public constructor(
        spec: String,
        main: String? = null,
        init: String? = null,
        step: String? = null,
        maxSamples: Int? = null,
        maxSteps: Int? = null,
        seed: String = genSeed(),
    ) : this(spec, main, init, step, maxSamples, maxSteps, seed, emptyList())

    override val nTraces: Int get() = maxSamplesOverride() ?: maxSamples ?: DEFAULT_TRACES

    override fun toCommand(tmpDir: Path): List<String> = buildList {
        add(quintExecutable()); add("run")
        add(resolveSpec(spec))
        add("--seed"); add(seed)
        add("--max-samples"); add(nTraces.toString())
        add("--n-traces"); add(nTraces.toString())
        add("--out-itf"); add(tmpDir.resolve("run_{seq}.itf.json").toString())
        add("--mbt")
        invariants.forEach { add("--invariants"); add(it) }
        add("--verbosity"); add(if (invariants.isEmpty()) "0" else "1")
        main?.let { add("--main"); add(it) }
        init?.let { add("--init"); add(it) }
        step?.let { add("--step"); add(it) }
        (maxStepsOverride() ?: maxSteps)?.let { add("--max-steps"); add(it.toString()) }
    }
}
