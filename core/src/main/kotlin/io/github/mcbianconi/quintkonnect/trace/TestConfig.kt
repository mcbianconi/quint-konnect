package io.github.mcbianconi.quintkonnect.trace

import java.nio.file.Path

// https://quint-lang.org/docs/cli#quint-test
public data class TestConfig(
    public val spec: String,
    public val test: String,
    public val main: String? = null,
    public val maxSamples: Int? = null,
    override val seed: String = genSeed(),
) : GeneratorConfig {

    override val nTraces: Int get() = maxSamplesOverride() ?: maxSamples ?: DEFAULT_TRACES

    override fun toCommand(tmpDir: Path): List<String> = buildList {
        add(quintExecutable()); add("test")
        add(resolveSpec(spec))
        add("--seed"); add(seed)
        add("--match"); add("^${escapeRegex(test)}$")
        add("--max-samples"); add(nTraces.toString())
        add("--out-itf"); add(tmpDir.resolve("test_{seq}.itf.json").toString())
        add("--verbosity"); add("0")
        main?.let { add("--main"); add(it) }
    }
}

// quint builds `new RegExp(match)` (no "u" flag) from this value, so only the
// ECMAScript SyntaxCharacters need a backslash; escaping anything else would
// be a SyntaxError under the "u" flag if quint ever adopts it.
private val REGEX_SYNTAX_CHARACTERS = "^$\\.*+?()[]{}|".toSet()

internal fun escapeRegex(value: String): String = buildString {
    for (c in value) {
        if (c in REGEX_SYNTAX_CHARACTERS) append('\\')
        append(c)
    }
}
