package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

class QuintRunTestGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {

    fun generate(clazz: KSClassDeclaration) {
        val packageName = clazz.packageName.asString()
        val className   = clazz.simpleName.asString()
        val outputName  = "${className}QuintRunTest"

        val annotation = clazz.annotations.first { it.shortName.asString() == "QuintRun" }
        val args       = annotation.arguments.associate { it.name!!.asString() to it.value }

        val spec       = args["spec"] as String
        val main       = (args["main"] as? String)?.takeIf { it.isNotBlank() }
        val init       = (args["init"] as? String)?.takeIf { it.isNotBlank() }
        val step       = (args["step"] as? String)?.takeIf { it.isNotBlank() }
        val maxSamples = (args["maxSamples"] as? Int)?.takeIf { it >= 0 }
        val maxSteps   = (args["maxSteps"] as? Int)?.takeIf { it >= 0 }
        val seed       = (args["seed"] as? String)?.takeIf { it.isNotBlank() }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, clazz.containingFile!!),
            packageName  = packageName,
            fileName     = outputName,
        )

        file.bufferedWriter().use { w ->
            w.appendLine("package $packageName")
            w.appendLine()
            w.appendLine("import io.github.mcbianconi.quintkonnect.Runner")
            w.appendLine("import io.github.mcbianconi.quintkonnect.trace.RunConfig")
            w.appendLine("import io.github.mcbianconi.quintkonnect.trace.genSeed")
            w.appendLine("import org.junit.jupiter.api.Test")
            w.appendLine()
            w.appendLine("class $outputName {")
            w.appendLine("    @Test")
            w.appendLine("    fun run() {")
            w.appendLine("        Runner.runTest(")
            w.appendLine("            driverFactory = { $className() },")
            w.appendLine("            generatorConfig = RunConfig(")
            w.appendLine("                spec = \"$spec\",")
            w.appendLine("                seed = ${if (seed != null) "\"$seed\"" else "genSeed()"},")
            if (main != null) w.appendLine("                main = \"$main\",")
            if (init != null) w.appendLine("                init = \"$init\",")
            if (step != null) w.appendLine("                step = \"$step\",")
            if (maxSamples != null) w.appendLine("                maxSamples = $maxSamples,")
            if (maxSteps != null) w.appendLine("                maxSteps = $maxSteps,")
            w.appendLine("            ),")
            w.appendLine("            testName = \"$className\",")
            w.appendLine("        )")
            w.appendLine("    }")
            w.appendLine("}")
        }

        logger.info("Generated $packageName.$outputName for $className")
    }
}
