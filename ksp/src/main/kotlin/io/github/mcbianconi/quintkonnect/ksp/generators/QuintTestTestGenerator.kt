package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

internal class QuintTestTestGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val adapterOption: String?,
) {

    private val testConfigClassName = ClassName("io.github.mcbianconi.quintkonnect.trace", "TestConfig")
    private val genSeedMember = MemberName("io.github.mcbianconi.quintkonnect.trace", "genSeed")

    fun generate(clazz: KSClassDeclaration) {
        val className = clazz.simpleName.asString()
        val outputName = "${className}QuintTestTest"

        val annotation = clazz.annotations.first { it.shortName.asString() == "QuintTest" }
        val args = annotation.arguments.associate { it.name!!.asString() to it.value }

        val spec = args["spec"] as String
        val test = args["test"] as String
        val main = (args["main"] as? String)?.takeIf { it.isNotBlank() }
        val maxSamples = (args["maxSamples"] as? Int)?.takeIf { it >= 0 }
        val seed = (args["seed"] as? String)?.takeIf { it.isNotBlank() }

        val configBlock = CodeBlock.builder()
            .add("%T(\n", testConfigClassName)
            .indent()
            .add("spec = %S,\n", spec)
            .add("test = %S,\n", test)
            .add("seed = %M(%S),\n", genSeedMember, seed.orEmpty())
        main?.let { configBlock.add("main = %S,\n", it) }
        maxSamples?.let { configBlock.add("maxSamples = %L,\n", it) }
        configBlock.unindent().add(")")

        QuintSuiteGenerator(codeGenerator, logger, adapterOption).generate(clazz, configBlock.build(), outputName)

        DriverManifestWriter.write(
            codeGenerator = codeGenerator,
            clazz = clazz,
            kind = "test",
            spec = spec,
            main = main,
            init = null,
            step = null,
            test = test,
            maxSamples = maxSamples,
            maxSteps = null,
            seed = seed,
            invariants = emptyList(),
        )
    }
}
