package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path

// https://quint-lang.org/docs/cli#quint-test
data class TestConfig(
    val spec: String,
    val test: String,
    val main: String? = null,
    val maxSamples: Int? = null,
    override val seed: String = genSeed(),
) : GeneratorConfig {

    override val nTraces: Int get() = maxSamples ?: DEFAULT_TRACES

    override fun toCommand(tmpDir: Path): List<String> = buildList {
        add("quint"); add("test")
        add(spec)
        add("--seed"); add(seed)
        add("--match"); add("^${test}$")
        add("--max-samples"); add(nTraces.toString())
        add("--out-itf"); add(tmpDir.resolve("test_{seq}.itf.json").toString())
        add("--verbosity"); add("0")
        main?.let { add("--main"); add(it) }
    }
}
