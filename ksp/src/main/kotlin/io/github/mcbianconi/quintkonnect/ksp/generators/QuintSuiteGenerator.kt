package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.mcbianconi.quintkonnect.ksp.ADAPTER_OPTION_NAME
import io.github.mcbianconi.quintkonnect.ksp.AdapterOption

/**
 * Shared by [QuintRunTestGenerator] and [QuintTestTestGenerator]: generates one runner-neutral
 * `object <Driver>QuintSuite : QuintSuite` per driver (holding the `RunConfig`/`TestConfig`
 * construction the caller builds into [configBlock]), plus a thin JUnit adapter
 * (`<Driver>QuintRunTest`/`<Driver>QuintTestTest`) that only delegates to it, unless the
 * `quintkonnect.adapter` processor option is `"none"`.
 */
internal class QuintSuiteGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val adapterOption: String?,
) {

    private val quintSuiteClassName = ClassName("io.github.mcbianconi.quintkonnect", "QuintSuite")
    private val traceReplayClassName = ClassName("io.github.mcbianconi.quintkonnect", "TraceReplay")
    private val replayRunnerClassName = ClassName("io.github.mcbianconi.quintkonnect", "ReplayRunner")
    private val replayListenerClassName = ClassName("io.github.mcbianconi.quintkonnect.listener", "ReplayListener")
    private val dynamicTestClassName = ClassName("org.junit.jupiter.api", "DynamicTest")
    private val testFactoryAnnotation = ClassName("org.junit.jupiter.api", "TestFactory")
    private val executionAnnotationClassName = ClassName("org.junit.jupiter.api.parallel", "Execution")
    private val executionModeClassName = ClassName("org.junit.jupiter.api.parallel", "ExecutionMode")

    /**
     * [configBlock] builds the driver's `RunConfig`/`TestConfig` (the caller's own concern);
     * [adapterOutputName] is the JUnit class name to use if the adapter is generated
     * (`<Driver>QuintRunTest`/`<Driver>QuintTestTest`, unchanged from before this suite existed).
     */
    fun generate(clazz: KSClassDeclaration, configBlock: CodeBlock, adapterOutputName: String) {
        val adapter = AdapterOption.resolve(adapterOption)
        if (adapter == null) {
            logger.error(
                "quint-konnect: invalid $ADAPTER_OPTION_NAME value \"$adapterOption\"; valid values are " +
                    "\"junit\" (the default) or \"none\".",
                clazz,
            )
            return
        }

        val packageName = clazz.packageName.asString()
        val className = clazz.simpleName.asString()
        val suiteOutputName = "${className}QuintSuite"

        writeSuite(clazz, packageName, className, suiteOutputName, configBlock)

        if (adapter == AdapterOption.JUNIT) {
            writeJunitAdapter(clazz, packageName, className, suiteOutputName, adapterOutputName)
        }
    }

    private fun writeSuite(
        clazz: KSClassDeclaration,
        packageName: String,
        className: String,
        suiteOutputName: String,
        configBlock: CodeBlock,
    ) {
        val nameProperty = PropertySpec.builder("name", STRING)
            .addModifiers(KModifier.OVERRIDE)
            .initializer("%S", className)
            .build()

        val tracesBody = CodeBlock.builder()
            .add("return %T(%L, listener = listener).traceReplays(\n", replayRunnerClassName, configBlock)
            .indent()
            .add("driverFactory = { %T() },\n", clazz.toClassName())
            .add("testName = %S,\n", className)
            .unindent()
            .add(")\n")
            .build()

        // An overriding function can't redeclare the interface's own default parameter value
        // (a Kotlin compile error); QuintSuite.traceReplays' default (ConsoleReplayListener())
        // still applies to callers that omit the argument.
        val tracesMethod = FunSpec.builder("traceReplays")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("listener", replayListenerClassName)
            .returns(LIST.parameterizedBy(traceReplayClassName))
            .addCode(tracesBody)
            .build()

        val typeSpec = TypeSpec.objectBuilder(suiteOutputName)
            .addOriginatingKSFile(clazz.containingFile!!)
            .addSuperinterface(quintSuiteClassName)
            .addProperty(nameProperty)
            .addFunction(tracesMethod)
            .build()

        FileSpec.builder(packageName, suiteOutputName)
            .addType(typeSpec)
            .build()
            .writeTo(codeGenerator, aggregating = false)

        logger.info("Generated $packageName.$suiteOutputName for $className")
    }

    private fun writeJunitAdapter(
        clazz: KSClassDeclaration,
        packageName: String,
        className: String,
        suiteOutputName: String,
        adapterOutputName: String,
    ) {
        val suiteClassName = ClassName(packageName, suiteOutputName)

        val tracesBody = CodeBlock.builder()
            .add(
                "return %T.traceReplays().map { %T.dynamicTest(it.displayName) { it.run() } }\n",
                suiteClassName,
                dynamicTestClassName,
            )
            .build()

        val tracesMethod = FunSpec.builder("traces")
            .addAnnotation(testFactoryAnnotation)
            .addAnnotation(
                AnnotationSpec.builder(executionAnnotationClassName)
                    .addMember("%T.CONCURRENT", executionModeClassName)
                    .build(),
            )
            .returns(LIST.parameterizedBy(dynamicTestClassName))
            .addCode(tracesBody)
            .build()

        val typeSpec = TypeSpec.classBuilder(adapterOutputName)
            .addOriginatingKSFile(clazz.containingFile!!)
            .addFunction(tracesMethod)
            .build()

        FileSpec.builder(packageName, adapterOutputName)
            .addType(typeSpec)
            .build()
            .writeTo(codeGenerator, aggregating = false)

        logger.info("Generated $packageName.$adapterOutputName for $className")
    }
}
