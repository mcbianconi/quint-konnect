package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

/**
 * Writes one JSON manifest per `@QuintRun`/`@QuintTest` driver under
 * `build/generated/ksp/<target>/test/resources/quintkonnect/traces-manifest/<package path>/
 * <Driver>-<kind>.json` (a plain resource file, not ".kt"/".java": [CodeGenerator.createNewFile]'s
 * `extensionName` decides that), so gradle-plugin's `generateQuintTraces` task (qk-adm2) can run
 * `quint` once per driver at Gradle-task time, without needing to read the SOURCE-retention
 * `@QuintRun`/`@QuintTest` annotation itself (a Gradle task can't).
 *
 * The `-<kind>` filename suffix names the manifest's kind (a driver takes only one of `@QuintRun`
 * and `@QuintTest`; the processor rejects both); nesting under the driver's own package path keeps two
 * same-named drivers in different packages from colliding on the manifest file itself (the
 * generated `build/quint-konnect/traces/<package.Driver>/` output directory this manifest feeds
 * is keyed by the fully qualified name too — see docs/decisions/generate-quint-traces-task.md).
 */
internal object DriverManifestWriter {

    fun write(
        codeGenerator: CodeGenerator,
        clazz: KSClassDeclaration,
        kind: String,
        spec: String,
        main: String?,
        init: String?,
        step: String?,
        test: String?,
        maxSamples: Int?,
        maxSteps: Int?,
        seed: String?,
        invariants: List<String>,
    ) {
        val json = buildJsonObject {
            put("driver", JsonPrimitive(clazz.qualifiedName?.asString() ?: clazz.simpleName.asString()))
            put("kind", JsonPrimitive(kind))
            put("spec", JsonPrimitive(spec))
            main?.let { put("main", JsonPrimitive(it)) }
            init?.let { put("init", JsonPrimitive(it)) }
            step?.let { put("step", JsonPrimitive(it)) }
            test?.let { put("test", JsonPrimitive(it)) }
            maxSamples?.let { put("maxSamples", JsonPrimitive(it)) }
            maxSteps?.let { put("maxSteps", JsonPrimitive(it)) }
            seed?.let { put("seed", JsonPrimitive(it)) }
            putJsonArray("invariants") { invariants.forEach { add(JsonPrimitive(it)) } }
        }

        val packagePath = clazz.packageName.asString().replace('.', '/')
        val className = clazz.simpleName.asString()
        codeGenerator.createNewFile(
            dependencies = Dependencies(false, clazz.containingFile!!),
            packageName = "quintkonnect/traces-manifest/$packagePath",
            fileName = "$className-$kind",
            extensionName = "json",
        ).use { it.write(Json.encodeToString(JsonObject.serializer(), json).toByteArray()) }
    }
}
