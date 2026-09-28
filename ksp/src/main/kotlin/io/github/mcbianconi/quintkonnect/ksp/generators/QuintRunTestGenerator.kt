package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

internal class QuintRunTestGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val adapterOption: String?,
) {

    private val runConfigClassName = ClassName("io.github.mcbianconi.quintkonnect.trace", "RunConfig")
    private val genSeedMember = MemberName("io.github.mcbianconi.quintkonnect.trace", "genSeed")

    fun generate(clazz: KSClassDeclaration) {
        val className = clazz.simpleName.asString()
        val outputName = "${className}QuintRunTest"

        val annotation = clazz.annotations.first { it.shortName.asString() == "QuintRun" }
        val args = annotation.arguments.associate { it.name!!.asString() to it.value }

        val spec = args["spec"] as String
        val main = (args["main"] as? String)?.takeIf { it.isNotBlank() }
        val init = (args["init"] as? String)?.takeIf { it.isNotBlank() }
        val step = (args["step"] as? String)?.takeIf { it.isNotBlank() }
        val maxSamples = (args["maxSamples"] as? Int)?.takeIf { it >= 0 }
        val maxSteps = (args["maxSteps"] as? Int)?.takeIf { it >= 0 }
        val seed = (args["seed"] as? String)?.takeIf { it.isNotBlank() }

        @Suppress("UNCHECKED_CAST")
        val invariants = (args["invariants"] as? List<String>).orEmpty().filter { it.isNotBlank() }

        val configBlock = CodeBlock.builder()
            .add("%T(\n", runConfigClassName)
            .indent()
            .add("spec = %S,\n", spec)
            .add("seed = %M(%S),\n", genSeedMember, seed.orEmpty())
        main?.let { configBlock.add("main = %S,\n", it) }
        init?.let { configBlock.add("init = %S,\n", it) }
        step?.let { configBlock.add("step = %S,\n", it) }
        maxSamples?.let { configBlock.add("maxSamples = %L,\n", it) }
        maxSteps?.let { configBlock.add("maxSteps = %L,\n", it) }
        if (invariants.isNotEmpty()) {
            val invariantsBlock = CodeBlock.builder().add("listOf(")
            invariants.forEachIndexed { i, invariant ->
                if (i > 0) invariantsBlock.add(", ")
                invariantsBlock.add("%S", invariant)
            }
            invariantsBlock.add(")")
            configBlock.add("invariants = %L,\n", invariantsBlock.build())
        }
        configBlock.unindent().add(")")

        QuintSuiteGenerator(codeGenerator, logger, adapterOption).generate(clazz, configBlock.build(), outputName)

        DriverManifestWriter.write(
            codeGenerator = codeGenerator,
            clazz = clazz,
            kind = "run",
            spec = spec,
            main = main,
            init = init,
            step = step,
            test = null,
            maxSamples = maxSamples,
            maxSteps = maxSteps,
            seed = seed,
            invariants = invariants,
        )
    }
}
