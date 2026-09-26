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
) : GeneratorConfig {

    override val nTraces: Int get() = maxSamples ?: DEFAULT_TRACES

    override fun toCommand(tmpDir: Path): List<String> = buildList {
        add("quint"); add("run")
        add(resolveSpec(spec))
        add("--seed"); add(seed)
        add("--max-samples"); add(nTraces.toString())
        add("--n-traces"); add(nTraces.toString())
        add("--out-itf"); add(tmpDir.resolve("run_{seq}.itf.json").toString())
        add("--mbt")
        add("--verbosity"); add("0")
        main?.let { add("--main"); add(it) }
        init?.let { add("--init"); add(it) }
        step?.let { add("--step"); add(it) }
        maxSteps?.let { add("--max-steps"); add(it.toString()) }
    }
}
