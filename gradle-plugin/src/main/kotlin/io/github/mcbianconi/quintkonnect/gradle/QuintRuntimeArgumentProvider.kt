package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

// Test.systemProperty()/systemProperties() aren't tracked as task inputs for up-to-date checking
// (Test/AbstractTestTask don't annotate their systemProperties getter @Input), so this is a
// CommandLineArgumentProvider instead: it lets each value be marked @Input (reruns Test tasks when
// it changes) or @Internal (machine-specific, e.g. a download path, excluded from that check)
// individually.
internal class QuintRuntimeArgumentProvider(
    @get:Input val downloadQuint: Provider<Boolean>,
    @get:Internal val quintExecutablePath: Provider<String>,
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> = buildList {
        if (downloadQuint.get()) {
            add("-D$QUINT_EXECUTABLE_SYSTEM_PROPERTY=${quintExecutablePath.get()}")
        }
    }
}

// Mirrors QUINT_EXECUTABLE_PROPERTY in core/.../trace/GeneratorConfig.kt: RunConfig/TestConfig
// resolve this system property instead of plain "quint" when a Test task sets it (internal
// visibility is per-module, so this can't just reference core's constant).
internal const val QUINT_EXECUTABLE_SYSTEM_PROPERTY: String = "quintkonnect.quintExecutable"
