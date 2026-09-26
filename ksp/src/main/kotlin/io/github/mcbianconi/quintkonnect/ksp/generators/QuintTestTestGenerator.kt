package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

internal class QuintTestTestGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {

    private val runnerClassName = ClassName("io.github.mcbianconi.quintkonnect", "Runner")
    private val testConfigClassName = ClassName("io.github.mcbianconi.quintkonnect.trace", "TestConfig")
    private val genSeedMember = MemberName("io.github.mcbianconi.quintkonnect.trace", "genSeed")
    private val testAnnotation = ClassName("org.junit.jupiter.api", "Test")

    fun generate(clazz: KSClassDeclaration) {
        val packageName = clazz.packageName.asString()
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
            .add("seed = %L,\n", if (seed != null) CodeBlock.of("%S", seed) else CodeBlock.of("%M()", genSeedMember))
        main?.let { configBlock.add("main = %S,\n", it) }
        maxSamples?.let { configBlock.add("maxSamples = %L,\n", it) }
        configBlock.unindent().add(")")

        val runBody = CodeBlock.builder()
            .add("%T.runTest(\n", runnerClassName)
            .indent()
            .add("driverFactory = { %T() },\n", clazz.toClassName())
            .add("generatorConfig = %L,\n", configBlock.build())
            .add("testName = %S,\n", className)
            .unindent()
            .add(")\n")
            .build()

        val runMethod = FunSpec.builder("run")
            .addAnnotation(testAnnotation)
            .addCode(runBody)
            .build()

        val typeSpec = TypeSpec.classBuilder(outputName)
            .addOriginatingKSFile(clazz.containingFile!!)
            .addFunction(runMethod)
            .build()

        val fileSpec = FileSpec.builder(packageName, outputName)
            .addType(typeSpec)
            .build()

        fileSpec.writeTo(codeGenerator, aggregating = false)

        logger.info("Generated $packageName.$outputName for $className")
    }
}
