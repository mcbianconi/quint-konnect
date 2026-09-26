@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package io.github.mcbianconi.quintkonnect.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.Step
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

internal fun compileWithProcessor(vararg sources: SourceFile): JvmCompilationResult {
    val compilation = KotlinCompilation().apply {
        this.sources = sources.toList()
        inheritClassPath = true
        // Match the jvmToolchain(21) all modules build with (quintkonnect.kotlin-jvm convention
        // plugin); the default (1.8) can't inline bytecode from :core/:itf/:annotations.
        jvmTarget = "21"
        useKsp2()
        symbolProcessorProviders = mutableListOf(QuintKonnectProcessorProvider())
        messageOutputStream = System.out
    }
    return compilation.compile()
}

internal fun kotlinSource(name: String, source: String): SourceFile = SourceFile.kotlin(name, source)
