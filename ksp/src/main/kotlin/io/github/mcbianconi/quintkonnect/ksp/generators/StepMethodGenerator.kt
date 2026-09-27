package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.writeTo

// A function and the @QuintAction annotation that applies to it, which may live on an
// overridee further up the class hierarchy (see `findQuintAction`). Shared with
// SpecActionValidator.kt (qk-75ad), which needs the same "what's this driver's @QuintAction
// surface" scan StepMethodGenerator already does.
internal class ActionMember(val function: KSFunctionDeclaration, val annotation: KSAnnotation) {
    val actionName: String by lazy {
        val nameArg = annotation.arguments.firstOrNull { it.name?.asString() == "name" }
        (nameArg?.value as? String)?.takeIf { it.isNotBlank() } ?: function.simpleName.asString()
    }
}

// Kotlin doesn't repeat an annotation on an override unless the source re-states it, but the
// action still applies: `getAllFunctions()` returns only the overriding declaration, so without
// this walk an overridden @QuintAction function would silently stop being an action. Each step
// of the walk is the closest overridee (`findOverridee()`), so a multi-level override chain
// (base -> mid -> derived) is followed all the way to whichever declaration carries the
// annotation.
internal fun KSFunctionDeclaration.findQuintAction(): KSAnnotation? {
    var current: KSFunctionDeclaration? = this
    val seen = mutableSetOf<KSFunctionDeclaration>()
    while (current != null && seen.add(current)) {
        val found = current.annotations.firstOrNull { it.shortName.asString() == "QuintAction" }
        if (found != null) return found
        current = current.findOverridee() as? KSFunctionDeclaration
    }
    return null
}

internal class StepMethodGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {

    private val stepClassName = ClassName("io.github.mcbianconi.quintkonnect", "Step")
    private val decodeMember = MemberName("io.github.mcbianconi.quintkonnect.nondet", "decode")
    private val decodeOrNullMember = MemberName("io.github.mcbianconi.quintkonnect.nondet", "decodeOrNull")
    private val runBlockingMember = MemberName("kotlinx.coroutines", "runBlocking")

    fun generate(clazz: KSClassDeclaration, resolver: Resolver) {
        val packageName = clazz.packageName.asString()
        val className = clazz.simpleName.asString()
        val outputName = "${className}Steps"
        val actions = clazz.getAllFunctions()
            .mapNotNull { fn -> fn.findQuintAction()?.let { ActionMember(fn, it) } }
            .toList()

        if (actions.isEmpty()) return

        val hasNonPublic = rejectNonPublicActions(actions)
        val hasDuplicate = rejectDuplicateActionNames(actions)
        val hasMissingRunBlocking = rejectMissingRunBlocking(actions, resolver)
        if (hasNonPublic || hasDuplicate || hasMissingRunBlocking) return

        val classTypeParams = clazz.typeParameters.toTypeParameterResolver()

        val whenBlock = CodeBlock.builder().beginControlFlow("when (step.actionTaken)")
        for (action in actions) {
            val fn = action.function
            val fnTypeParams = fn.typeParameters.toTypeParameterResolver(parent = classTypeParams)

            whenBlock.beginControlFlow("%S ->", action.actionName)
            for (param in fn.parameters) {
                val paramName = param.name!!.asString()
                val resolvedType = param.type.resolve()
                val typeName = param.type.toTypeName(fnTypeParams)
                val decodeMemberName = if (resolvedType.isMarkedNullable) decodeOrNullMember else decodeMember
                whenBlock.addStatement(
                    "val %N = step.nondetPicks.%M<%T>(%S)",
                    paramName, decodeMemberName, typeName, paramName,
                )
            }
            val args = fn.parameters.map { CodeBlock.of("%N", it.name!!.asString()) }.joinToCode(", ")
            if (fn.modifiers.contains(Modifier.SUSPEND)) {
                // generatedStep itself stays non-suspend (Driver.step's reflective dispatch calls
                // it as a plain function); runBlocking bridges into the suspend action instead.
                // Its block has an implicit CoroutineScope receiver, which would shadow `this`, so
                // the driver receiver needs the explicit `this@generatedStep` label.
                //
                // Not kotlinx.coroutines.test.runTest: it starts a fresh TestScope per call, so
                // virtual time would reset every step instead of advancing across a trace, and it
                // needs kotlinx-coroutines-test, which a driver module has no other reason to
                // depend on. runBlocking uses real time (a suspend action's `delay` costs wall
                // clock time during replay) but keeps a single, real dispatcher across the whole
                // trace.
                whenBlock.beginControlFlow("%M", runBlockingMember)
                whenBlock.addStatement("this@generatedStep.%N(%L)", fn.simpleName.asString(), args)
                whenBlock.endControlFlow()
            } else {
                whenBlock.addStatement("this.%N(%L)", fn.simpleName.asString(), args)
            }
            whenBlock.endControlFlow()
        }
        whenBlock.addStatement("else -> error(\"Unimplemented action: \${step.actionTaken}\")")
        whenBlock.endControlFlow()

        val generatedStep = FunSpec.builder("generatedStep")
            .receiver(clazz.toClassName())
            .addParameter("step", stepClassName)
            .addOriginatingKSFile(clazz.containingFile!!)
            .addCode(whenBlock.build())
            .build()

        val fileSpec = FileSpec.builder(packageName, outputName)
            .addFunction(generatedStep)
            .build()

        fileSpec.writeTo(codeGenerator, aggregating = false)

        logger.info("Generated $packageName.$outputName for $className")
    }

    // qk-9lsz: a @QuintAction function must be callable from the generated dispatcher file, which
    // is a different file from the driver's (and, for an inherited action, a different package).
    private fun rejectNonPublicActions(actions: List<ActionMember>): Boolean {
        var hasNonPublic = false
        for (action in actions) {
            if (!action.function.isPublic()) {
                hasNonPublic = true
                logger.error(
                    "@QuintAction function \"${action.function.simpleName.asString()}\" must be public. " +
                        "The generated dispatcher calls it from a separate file.",
                    action.function,
                )
            }
        }
        return hasNonPublic
    }

    // qk-nqry: Kotlin's `when` doesn't reject duplicate branch labels, so without this check the
    // first branch would silently shadow the second at runtime.
    private fun rejectDuplicateActionNames(actions: List<ActionMember>): Boolean {
        var hasDuplicate = false
        for ((actionName, group) in actions.groupBy { it.actionName }) {
            if (group.size > 1) {
                hasDuplicate = true
                val functionNames = group.joinToString(", ") { it.function.simpleName.asString() }
                logger.error(
                    "Duplicate @QuintAction name \"$actionName\" on functions: $functionNames. " +
                        "Each action name must be unique within a driver class.",
                    group[1].function,
                )
            }
        }
        return hasDuplicate
    }

    // qk-33ky: a suspend @QuintAction's generated call needs kotlinx.coroutines.runBlocking on
    // the driver module's own classpath (generated code runs there, not in :core, which doesn't
    // depend on kotlinx-coroutines). Report that plainly instead of an unresolved-reference error
    // in generated code the user never wrote.
    private fun rejectMissingRunBlocking(actions: List<ActionMember>, resolver: Resolver): Boolean {
        val firstSuspendAction = actions.firstOrNull { it.function.modifiers.contains(Modifier.SUSPEND) }
            ?: return false

        val runBlockingResolvable = resolver
            .getFunctionDeclarationsByName(resolver.getKSNameFromString("kotlinx.coroutines.runBlocking"), includeTopLevel = true)
            .any()
        if (runBlockingResolvable) return false

        logger.error(
            "@QuintAction function \"${firstSuspendAction.function.simpleName.asString()}\" is " +
                "suspend, so the generated dispatcher needs kotlinx.coroutines.runBlocking to " +
                "call it, but kotlinx-coroutines-core isn't on this module's compile classpath. " +
                "Add it, e.g. testImplementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:<version>\").",
            firstSuspendAction.function,
        )
        return true
    }
}
