package io.github.mcbianconi.quintkonnect.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import javax.inject.Inject

// https://quint-lang.org/docs/getting-started#installation
public const val QUINT_INSTALL_COMMAND: String = "npm i -g @informalsystems/quint"

@DisableCachingByDefault(because = "Checks the environment's installed quint CLI, not this project's inputs.")
public abstract class CheckQuintTask : DefaultTask() {

    @get:Input
    public abstract val expectedVersion: Property<String>

    // Not exposed through the quintKonnect extension: this exists so a test can point it at a
    // nonexistent name to exercise the "quint not found" path deterministically, without relying
    // on hiding the real quint executable from PATH (Gradle daemons can outlive and be reused
    // across separate builds with different requested environments).
    @get:Input
    public abstract val quintExecutable: Property<String>

    @get:Inject
    protected abstract val execOperations: ExecOperations

    init {
        group = "verification"
        description = "Checks that quint is installed and matches the configured version."
        quintExecutable.convention("quint")
        // No @OutputFile/@OutputDirectory property: a task with no declared outputs is never
        // up-to-date, so this runs (and warns) on every invocation, not just when
        // expectedVersion changes.
    }

    @TaskAction
    public fun check() {
        val stdout = ByteArrayOutputStream()
        val exitValue = try {
            execOperations.exec {
                it.commandLine(quintExecutable.get(), "--version")
                it.standardOutput = stdout
                it.isIgnoreExitValue = true
            }.exitValue
        } catch (e: Exception) {
            throw GradleException(
                "quint was not found on PATH. Install it with: $QUINT_INSTALL_COMMAND@${expectedVersion.get()}",
                e,
            )
        }

        if (exitValue != 0) {
            throw GradleException("quint --version exited with code $exitValue")
        }

        val actual = stdout.toString(Charsets.UTF_8).trim()
        val expected = expectedVersion.get()
        if (actual != expected) {
            logger.warn(
                "quint $actual is installed, but this project is configured for $expected. " +
                    "Update it with: $QUINT_INSTALL_COMMAND@$expected",
            )
        }
    }
}
