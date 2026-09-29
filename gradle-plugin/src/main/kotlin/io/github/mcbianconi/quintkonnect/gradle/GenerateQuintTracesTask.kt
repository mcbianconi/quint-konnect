package io.github.mcbianconi.quintkonnect.gradle

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

// One @QuintRun/@QuintTest driver's manifest (ksp/.../generators/DriverManifestWriter.kt): what
// this task needs to run `quint` for it, read back from the KSP-generated JSON resource.
internal data class DriverManifest(
    val driver: String,
    val kind: String,
    val spec: String,
    val main: String?,
    val init: String?,
    val step: String?,
    val test: String?,
    val maxSamples: Int?,
    val maxSteps: Int?,
    val seed: String?,
    val invariants: List<String>,
)

internal fun parseDriverManifest(file: File): DriverManifest {
    val root = Json.parseToJsonElement(file.readText()).jsonObject
    return DriverManifest(
        driver = root.getValue("driver").jsonPrimitive.content,
        kind = root.getValue("kind").jsonPrimitive.content,
        spec = root.getValue("spec").jsonPrimitive.content,
        main = root["main"]?.jsonPrimitive?.contentOrNull,
        init = root["init"]?.jsonPrimitive?.contentOrNull,
        step = root["step"]?.jsonPrimitive?.contentOrNull,
        test = root["test"]?.jsonPrimitive?.contentOrNull,
        maxSamples = root["maxSamples"]?.jsonPrimitive?.intOrNull,
        maxSteps = root["maxSteps"]?.jsonPrimitive?.intOrNull,
        seed = root["seed"]?.jsonPrimitive?.contentOrNull,
        invariants = root["invariants"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
    )
}

// "/" is the only character sanitized here too (mirrors FailureTraceWriter.kt's sanitizeFileName):
// a driver's class name shouldn't be able to escape the output directory.
private fun sanitizeFileName(name: String): String = name.replace("/", "_")

/**
 * Runs `quint` once per `@QuintRun`/`@QuintTest` driver (from KSP's per-driver [manifests]) into
 * `<[outputDir]>/<driver's fully qualified class name>/`, so Test tasks can replay saved traces
 * ([io.github.mcbianconi.quintkonnect.trace.TracesDirTraceSource] in `core`, wired in by
 * [QuintKonnectPlugin]) instead of invoking `quint` themselves (qk-adm2).
 *
 * Cacheable only when every driver's seed is pinned (an explicit override, `QUINT_SEED`, or the
 * driver's own `@QuintRun`/`@QuintTest` `seed`): an unpinned seed is resolved fresh inside [run]
 * (mirroring `genSeed()`'s random fallback, core/.../trace/Seed.kt) each time this task actually
 * executes, which would never be reproducible as a cache key (see
 * docs/decisions/generate-quint-traces-task.md).
 *
 * A driver for which `quint` exits non-zero doesn't fail this task (an invariant violation is a
 * normal, expected outcome, not a build failure): its exit is recorded as
 * `<driver>/error.txt` instead of trace files, and [TracesDirTraceSource] throws with that text at
 * replay time, so it still surfaces as that driver's own test failure, same as it would running
 * `quint` at test time (`TraceGenerator`).
 */
@CacheableTask
public abstract class GenerateQuintTracesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    public abstract val manifests: ConfigurableFileCollection

    // The spec files' content (a manifest holds only the spec path): without this, a spec change
    // with a pinned seed left the task UP-TO-DATE/FROM-CACHE and replayed stale traces (qk-q5z8).
    // Set from quintIrSpecs (which also covers imported .qnt files) and each manifest's spec.
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val specs: ConfigurableFileCollection

    // Only used to resolve a relative manifest `spec` the same way a relative `spec` resolves
    // elsewhere (QuintIrTask.kt); not a build input in its own right.
    @get:Internal
    public abstract val projectDirectory: Property<String>

    // The resolved path, not the semantic version: two different absolute paths to the same quint
    // version should hit the same cache entry (unlike QuintIrTask.quintExecutable, which is @Input
    // and so isn't relocatable — not revisited here). quintVersion below is the real cache-relevant
    // input.
    @get:Internal
    public abstract val quintExecutable: Property<String>

    @get:Input
    public abstract val quintVersion: Property<String>

    @get:Input @get:Optional
    public abstract val maxSamplesOverride: Property<Int>

    @get:Input @get:Optional
    public abstract val maxStepsOverride: Property<Int>

    @get:Input @get:Optional
    public abstract val seedOverride: Property<String>

    @get:Input @get:Optional
    public abstract val envSeed: Property<String>

    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    @get:Inject
    protected abstract val execOperations: ExecOperations

    init {
        group = "verification"
        description = "Runs `quint` once per @QuintRun/@QuintTest driver into build/quint-konnect/traces/, " +
            "so Test tasks can replay saved traces instead of running quint themselves."
        outputs.upToDateWhen { everySeedIsPinned() }
        outputs.cacheIf { everySeedIsPinned() }
    }

    // A random per-driver seed (below) is only reproducible for as long as this task's own cached
    // output already has it baked in: it can never be recomputed identically on a cache miss, so a
    // build with any unpinned driver must always re-execute instead of relying on UP-TO-DATE/
    // FROM-CACHE (see the class doc and docs/decisions/generate-quint-traces-task.md).
    private fun everySeedIsPinned(): Boolean {
        if (seedOverride.isPresent || envSeed.isPresent) return true
        return readManifests().all { !it.seed.isNullOrBlank() }
    }

    private fun readManifests(): List<DriverManifest> = manifests.files.filter { it.isFile }.map(::parseDriverManifest)

    @TaskAction
    public fun run() {
        val outDir = outputDir.get().asFile
        outDir.deleteRecursively() // stale runs from a smaller maxSamples/maxSteps must not linger
        outDir.mkdirs()
        val root = File(projectDirectory.get())

        readManifests().forEach { manifest -> runDriver(manifest, outDir, root) }
    }

    private fun runDriver(manifest: DriverManifest, outDir: File, root: File) {
        // The fully qualified name matches the generated suite's testName, which
        // defaultTraceSource (core/.../trace/TraceSource.kt) looks up the same way at test time.
        val driverDir = File(outDir, sanitizeFileName(manifest.driver))
        driverDir.mkdirs()

        val seed = seedOverride.orNull
            ?: manifest.seed?.takeIf { it.isNotBlank() }
            ?: envSeed.orNull
            ?: "0x%x".format((Math.random() * Int.MAX_VALUE).toLong())
        File(driverDir, "seed.txt").writeText(seed)

        val nTraces = maxSamplesOverride.orNull ?: manifest.maxSamples ?: 100
        val command = buildCommand(manifest, seed, nTraces, driverDir, root)

        val stderr = ByteArrayOutputStream()
        val exitValue = execOperations.exec {
            it.commandLine(command)
            it.standardOutput = ByteArrayOutputStream()
            it.errorOutput = stderr
            it.isIgnoreExitValue = true
        }.exitValue

        if (exitValue != 0) {
            File(driverDir, "error.txt").writeText(
                buildErrorMessage(manifest.invariants, seed, stderr.toString(Charsets.UTF_8), exitValue),
            )
        }
    }

    // Mirrors RunConfig/TestConfig.toCommand (core/.../trace/RunConfig.kt, TestConfig.kt): can't
    // call those directly here (this runs in the Gradle daemon, not a forked Test JVM — they read
    // quintkonnect.* system properties and a relative `spec` against the *process'* own state,
    // which is the daemon's, not this task's).
    private fun buildCommand(manifest: DriverManifest, seed: String, nTraces: Int, driverDir: File, root: File): List<String> {
        val spec = resolveManifestSpec(manifest.spec, root)
        return buildList {
            add(quintExecutable.get())
            if (manifest.kind == "test") {
                add("test")
                add(spec)
                add("--seed")
                add(seed)
                add("--match")
                add("^${escapeTestRegex(manifest.test!!)}$")
                add("--max-samples")
                add(nTraces.toString())
                add("--out-itf")
                add(File(driverDir, "test_{seq}.itf.json").absolutePath)
                add("--verbosity")
                add("0")
                manifest.main?.let {
                    add("--main")
                    add(it)
                }
            } else {
                add("run")
                add(spec)
                add("--seed")
                add(seed)
                add("--max-samples")
                add(nTraces.toString())
                add("--n-traces")
                add(nTraces.toString())
                add("--out-itf")
                add(File(driverDir, "run_{seq}.itf.json").absolutePath)
                add("--mbt")
                manifest.invariants.forEach {
                    add("--invariants")
                    add(it)
                }
                add("--verbosity")
                add(if (manifest.invariants.isEmpty()) "0" else "1")
                manifest.main?.let {
                    add("--main")
                    add(it)
                }
                manifest.init?.let {
                    add("--init")
                    add(it)
                }
                manifest.step?.let {
                    add("--step")
                    add(it)
                }
                (maxStepsOverride.orNull ?: manifest.maxSteps)?.let {
                    add("--max-steps")
                    add(it.toString())
                }
            }
        }
    }
}

// Mirrors resolveSpec (core/.../trace/GeneratorConfig.kt).
internal fun resolveSpecFile(spec: String, root: File): File {
    val specFile = File(spec)
    return if (specFile.isAbsolute) specFile else File(root, spec)
}

private fun resolveManifestSpec(spec: String, root: File): String = resolveSpecFile(spec, root).path

// Mirrors escapeRegex (core/.../trace/TestConfig.kt): quint builds `new RegExp(match)` (no "u"
// flag) from this value, so only the ECMAScript SyntaxCharacters need a backslash.
private val REGEX_SYNTAX_CHARACTERS = "^$\\.*+?()[]{}|".toSet()

private fun escapeTestRegex(value: String): String = buildString {
    for (c in value) {
        if (c in REGEX_SYNTAX_CHARACTERS) append('\\')
        append(c)
    }
}

// Mirrors TraceGenerator's non-zero-exit handling (core/.../trace/TraceGenerator.kt), simplified:
// this runs in the Gradle daemon, without :core on the classpath, so it doesn't parse the
// generated ITF trace to include a violating-trace dump the way TraceGenerator's own message does.
private fun buildErrorMessage(invariants: List<String>, seed: String, stderr: String, exitValue: Int): String =
    if (invariants.isNotEmpty() && stderr.contains("Invariant violated")) {
        "Quint invariant violated: ${invariants.joinToString(", ")} (seed $seed)"
    } else {
        "Quint returned non-zero exit code ($exitValue).\n$stderr"
    }
