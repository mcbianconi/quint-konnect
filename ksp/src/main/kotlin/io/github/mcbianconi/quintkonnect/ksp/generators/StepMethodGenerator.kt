package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
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

    fun generate(clazz: KSClassDeclaration) {
        val packageName = clazz.packageName.asString()
        val className = clazz.simpleName.asString()
        val outputName = "${className}Steps"
        val annotatedFns = clazz.getDeclaredFunctions()
            .filter { it.annotations.any { a -> a.shortName.asString() == "QuintAction" } }
            .toList()

        if (annotatedFns.isEmpty()) return

        val classTypeParams = clazz.typeParameters.toTypeParameterResolver()

        val whenBlock = CodeBlock.builder().beginControlFlow("when (step.actionTaken)")
        for (fn in annotatedFns) {
            val fnTypeParams = fn.typeParameters.toTypeParameterResolver(parent = classTypeParams)

            whenBlock.beginControlFlow("%S ->", fn.actionName())
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

    private fun KSFunctionDeclaration.actionName(): String {
        val actionAnnotation = annotations.first { it.shortName.asString() == "QuintAction" }
        val nameArg = actionAnnotation.arguments.firstOrNull { it.name?.asString() == "name" }
        return (nameArg?.value as? String)?.takeIf { it.isNotBlank() } ?: simpleName.asString()
    }
}
