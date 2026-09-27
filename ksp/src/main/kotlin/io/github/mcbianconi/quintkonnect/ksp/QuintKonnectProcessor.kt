package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import io.github.mcbianconi.quintkonnect.ksp.generators.QuintRunTestGenerator
import io.github.mcbianconi.quintkonnect.ksp.generators.QuintTestTestGenerator
import io.github.mcbianconi.quintkonnect.ksp.generators.StepMethodGenerator
import io.github.mcbianconi.quintkonnect.ksp.ir.loadQuintIrModule
import java.io.File

internal class QuintKonnectProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    // Set from the "quintkonnect.irDir" processor option (QuintKonnectProcessorProvider.kt) when
    // the Gradle plugin's quintIr task is wired up; null (the default outside that plugin, e.g.
    // example's manual KSP wiring) means IR lookup is skipped entirely, unchanged from before qk-8i6m.
    private val irDir: File?,
) : SymbolProcessor {

    private val quintRunFqn  = "io.github.mcbianconi.quintkonnect.annotations.QuintRun"
    private val quintTestFqn = "io.github.mcbianconi.quintkonnect.annotations.QuintTest"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(quintRunFqn)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { clazz ->
                if (validateNoArgConstructor(clazz, logger)) return@forEach
                if (validateQuintStateType(clazz, resolver, logger)) return@forEach
                StepMethodGenerator(codeGenerator, logger).generate(clazz, resolver)
                QuintRunTestGenerator(codeGenerator, logger).generate(clazz)
                loadIr(clazz, "QuintRun")
            }

        resolver.getSymbolsWithAnnotation(quintTestFqn)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { clazz ->
                if (validateNoArgConstructor(clazz, logger)) return@forEach
                if (validateQuintStateType(clazz, resolver, logger)) return@forEach
                StepMethodGenerator(codeGenerator, logger).generate(clazz, resolver)
                QuintTestTestGenerator(codeGenerator, logger).generate(clazz)
                loadIr(clazz, "QuintTest")
            }

        return emptyList()
    }

    // Loads the driver's spec IR and, for @QuintRun (not @QuintTest: see validateAgainstSpec's
    // own doc comment), checks its @QuintAction surface against it (qk-75ad); qk-ixox will also
    // read this IR to generate types.
    private fun loadIr(clazz: KSClassDeclaration, annotationShortName: String) {
        val dir = irDir ?: return
        val annotation = clazz.annotations.first { it.shortName.asString() == annotationShortName }
        val args = annotation.arguments.associate { it.name!!.asString() to it.value }
        val spec = args["spec"] as? String ?: return
        val main = (args["main"] as? String)?.takeIf { it.isNotBlank() }

        val module = try {
            loadQuintIrModule(dir, spec, main)
        } catch (e: Exception) {
            logger.warn("quint-konnect: could not parse quint IR for spec \"$spec\": ${e.message}", clazz)
            return
        }
        if (module == null) {
            logger.warn("quint-konnect: no quint IR found for spec \"$spec\" under $dir", clazz)
            return
        }
        if (annotationShortName == "QuintRun" && !overridesNondetExtraction(clazz)) {
            val init = (args["init"] as? String)?.takeIf { it.isNotBlank() } ?: "init"
            val step = (args["step"] as? String)?.takeIf { it.isNotBlank() } ?: "step"
            validateAgainstSpec(clazz, module, init, step, logger)
        }
    }
}
