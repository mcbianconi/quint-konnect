@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Step
import java.io.File
import java.lang.reflect.Constructor

// `Step`/`NondetPicks` constructors are `internal` to :core, so they compile down to public
// bytecode but aren't reachable from Kotlin source in another module (see qk-49by report:
// javap confirms `internal` is a source-only restriction on the JVM). Reflection here builds
// a real `Step` the way the runtime does, without depending on :core's internal API surface
// from Kotlin source.
private val stepCtor: Constructor<*> = Class.forName("io.github.mcbianconi.quintkonnect.Step")
    .getDeclaredConstructor(
        String::class.java,
        Class.forName("io.github.mcbianconi.quintkonnect.nondet.NondetPicks"),
        ItfValue::class.java,
    )

private val nondetPicksCtor: Constructor<*> = Class.forName("io.github.mcbianconi.quintkonnect.nondet.NondetPicks")
    .getDeclaredConstructor(LinkedHashMap::class.java)

internal fun testStep(
    actionTaken: String,
    picks: Map<String, ItfValue> = emptyMap(),
    state: ItfValue = ItfValue.Record(LinkedHashMap()),
): Step {
    val nondetPicks = nondetPicksCtor.newInstance(LinkedHashMap(picks))
    return stepCtor.newInstance(actionTaken, nondetPicks, state) as Step
}

internal fun compileWithProcessor(
    vararg sources: SourceFile,
    kspOptions: Map<String, String> = emptyMap(),
): JvmCompilationResult {
    val compilation = KotlinCompilation().apply {
        this.sources = sources.toList()
        inheritClassPath = true
        // Match the jvmToolchain(21) all modules build with (quintkonnect.kotlin-jvm convention
        // plugin); the default (1.8) can't inline bytecode from :core/:itf/:annotations.
        jvmTarget = "21"
        useKsp2()
        symbolProcessorProviders = mutableListOf(QuintKonnectProcessorProvider())
        messageOutputStream = System.out
        kspProcessorOptions = kspOptions.toMutableMap()
    }
    return compilation.compile()
}

internal fun kotlinSource(name: String, source: String): SourceFile = SourceFile.kotlin(name, source)

// qk-33ky: proves the processor's own "kotlinx.coroutines.runBlocking isn't resolvable" error
// fires. `compileWithProcessor`'s inheritClassPath = true always carries a transitive
// kotlinx-coroutines-core (the Kotlin compiler tooling itself depends on it), so that path can't
// produce a classpath without it; rebuild this process' own classpath from `java.class.path`
// (populated with real per-jar entries under Gradle's test worker) with every kotlinx-coroutines
// jar filtered out instead.
internal fun compileWithProcessorWithoutCoroutines(vararg sources: SourceFile): JvmCompilationResult {
    val classpathWithoutCoroutines = System.getProperty("java.class.path")
        .split(File.pathSeparatorChar)
        .map(::File)
        .filterNot { it.name.contains("kotlinx-coroutines") }
    val compilation = KotlinCompilation().apply {
        this.sources = sources.toList()
        inheritClassPath = false
        classpaths = classpathWithoutCoroutines
        jvmTarget = "21"
        useKsp2()
        symbolProcessorProviders = mutableListOf(QuintKonnectProcessorProvider())
        messageOutputStream = System.out
    }
    return compilation.compile()
}
