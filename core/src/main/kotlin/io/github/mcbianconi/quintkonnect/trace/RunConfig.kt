package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path

// https://quint-lang.org/docs/cli#quint-run
data class RunConfig(
    val spec: String,
    val main: String? = null,
    val init: String? = null,
    val step: String? = null,
    val maxSamples: Int? = null,
    val maxSteps: Int? = null,
    override val seed: String = genSeed(),
) : GeneratorConfig {

    override val nTraces: Int get() = maxSamples ?: DEFAULT_TRACES

    override fun toCommand(tmpDir: Path): List<String> = buildList {
        add("quint"); add("run")
        add(spec)
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
