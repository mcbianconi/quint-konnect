package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*

class StepMethodGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {

    fun generate(clazz: KSClassDeclaration) {
        val packageName  = clazz.packageName.asString()
        val className    = clazz.simpleName.asString()
        val outputName   = "${className}Steps"
        val annotatedFns = clazz.getDeclaredFunctions()
            .filter { it.annotations.any { a -> a.shortName.asString() == "QuintAction" } }
            .toList()

        if (annotatedFns.isEmpty()) return

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, clazz.containingFile!!),
            packageName  = packageName,
            fileName     = outputName,
        )

        file.bufferedWriter().use { w ->
            w.appendLine("package $packageName")
            w.appendLine()
            w.appendLine("import io.github.mcbianconi.quintkonnect.Step")
            w.appendLine("import io.github.mcbianconi.quintkonnect.nondet.decode")
            w.appendLine("import io.github.mcbianconi.quintkonnect.nondet.decodeOrNull")
            w.appendLine()
            w.appendLine("fun $className.generatedStep(step: Step) {")
            w.appendLine("    when (step.actionTaken) {")

            for (fn in annotatedFns) {
                val actionAnnotation = fn.annotations.first { it.shortName.asString() == "QuintAction" }
                val nameArg = actionAnnotation.arguments.firstOrNull { it.name?.asString() == "name" }
                val actionName = (nameArg?.value as? String)?.takeIf { it.isNotBlank() }
                    ?: fn.simpleName.asString()

                w.appendLine("        \"$actionName\" -> {")
                for (param in fn.parameters) {
                    val paramName = param.name!!.asString()
                    val typeName  = param.type.resolve().typeName()
                    val isNullable = param.type.resolve().isMarkedNullable
                    if (isNullable) {
                        w.appendLine("            val $paramName = step.nondetPicks.decodeOrNull<$typeName>(\"$paramName\")")
                    } else {
                        w.appendLine("            val $paramName = step.nondetPicks.decode<$typeName>(\"$paramName\")")
                    }
                }
                val args = fn.parameters.joinToString(", ") { it.name!!.asString() }
                w.appendLine("            this.${fn.simpleName.asString()}($args)")
                w.appendLine("        }")
            }

            w.appendLine("        else -> error(\"Unimplemented action: \${step.actionTaken}\")")
            w.appendLine("    }")
            w.appendLine("}")
        }

        logger.info("Generated $packageName.$outputName for $className")
    }

    private fun KSType.typeName(): String {
        val base = (declaration.qualifiedName ?: declaration.simpleName).asString()
        val nullable = if (isMarkedNullable) "?" else ""
        return if (arguments.isEmpty()) {
            "$base$nullable"
        } else {
            val args = arguments.joinToString(", ") { arg ->
                when (arg.variance) {
                    Variance.STAR -> "*"
                    else          -> arg.type?.resolve()?.typeName() ?: "*"
                }
            }
            "$base<$args>$nullable"
        }
    }
}
