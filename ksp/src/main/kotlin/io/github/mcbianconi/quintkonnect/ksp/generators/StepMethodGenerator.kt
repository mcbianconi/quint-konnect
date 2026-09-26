package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
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

internal class StepMethodGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {

    private val stepClassName = ClassName("io.github.mcbianconi.quintkonnect", "Step")
    private val decodeMember = MemberName("io.github.mcbianconi.quintkonnect.nondet", "decode")
    private val decodeOrNullMember = MemberName("io.github.mcbianconi.quintkonnect.nondet", "decodeOrNull")

    // A function and the @QuintAction annotation that applies to it, which may live on an
    // overridee further up the class hierarchy (see `findQuintAction`).
    private class ActionMember(val function: KSFunctionDeclaration, val annotation: KSAnnotation) {
        val actionName: String by lazy {
            val nameArg = annotation.arguments.firstOrNull { it.name?.asString() == "name" }
            (nameArg?.value as? String)?.takeIf { it.isNotBlank() } ?: function.simpleName.asString()
        }
    }

    fun generate(clazz: KSClassDeclaration) {
        val packageName = clazz.packageName.asString()
        val className = clazz.simpleName.asString()
        val outputName = "${className}Steps"
        val actions = clazz.getAllFunctions()
            .mapNotNull { fn -> fn.findQuintAction()?.let { ActionMember(fn, it) } }
            .toList()

        if (actions.isEmpty()) return

        val hasNonPublic = rejectNonPublicActions(actions)
        val hasDuplicate = rejectDuplicateActionNames(actions)
        if (hasNonPublic || hasDuplicate) return

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
            whenBlock.addStatement("this.%N(%L)", fn.simpleName.asString(), args)
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

    // Kotlin doesn't repeat an annotation on an override unless the source re-states it, but the
    // action still applies: `getAllFunctions()` returns only the overriding declaration, so without
    // this walk an overridden @QuintAction function would silently stop being an action. Each step
    // of the walk is the closest overridee (`findOverridee()`), so a multi-level override chain
    // (base -> mid -> derived) is followed all the way to whichever declaration carries the
    // annotation.
    private fun KSFunctionDeclaration.findQuintAction(): KSAnnotation? {
        var current: KSFunctionDeclaration? = this
        val seen = mutableSetOf<KSFunctionDeclaration>()
        while (current != null && seen.add(current)) {
            val found = current.annotations.firstOrNull { it.shortName.asString() == "QuintAction" }
            if (found != null) return found
            current = current.findOverridee() as? KSFunctionDeclaration
        }
        return null
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
}
