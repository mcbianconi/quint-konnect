package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import io.github.mcbianconi.quintkonnect.ksp.generators.QuintRunTestGenerator
import io.github.mcbianconi.quintkonnect.ksp.generators.QuintTestTestGenerator
import io.github.mcbianconi.quintkonnect.ksp.generators.SpecTypesGenerator
import io.github.mcbianconi.quintkonnect.ksp.generators.StepMethodGenerator
import io.github.mcbianconi.quintkonnect.ksp.ir.QuintModuleIr
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

    private val specTypesGenerator = SpecTypesGenerator(codeGenerator, logger)

    // Keyed by driver FQN: a driver deferred to the next round (see process) keeps the IR it
    // loaded, so its "no quint IR found" warning isn't reported twice.
    private val irByDriver = mutableMapOf<String, QuintModuleIr?>()
    private val deferredOnce = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()

        resolver.getSymbolsWithAnnotation(quintRunFqn)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { clazz ->
                val module = loadIr(clazz, "QuintRun")
                if (module != null && defer(clazz)) { deferred += clazz; return@forEach }
                if (validateNoArgConstructor(clazz, logger)) return@forEach
                if (validateQuintStateType(clazz, resolver, logger)) return@forEach
                StepMethodGenerator(codeGenerator, logger).generate(clazz, resolver)
                QuintRunTestGenerator(codeGenerator, logger).generate(clazz)
                if (module != null) validateRunDriver(clazz, module)
            }

        resolver.getSymbolsWithAnnotation(quintTestFqn)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { clazz ->
                val module = loadIr(clazz, "QuintTest")
                if (module != null && defer(clazz)) { deferred += clazz; return@forEach }
                if (validateNoArgConstructor(clazz, logger)) return@forEach
                if (validateQuintStateType(clazz, resolver, logger)) return@forEach
                StepMethodGenerator(codeGenerator, logger).generate(clazz, resolver)
                QuintTestTestGenerator(codeGenerator, logger).generate(clazz)
            }

        specTypesGenerator.flush()
        return deferred
    }

    // A driver whose signatures reference a type this processor generates (qk-ixox: a
    // `<Module>Spec.Player` @QuintAction parameter) sees it as an error type until the next
    // round. Deferring once lets that round resolve it; a driver still invalid after that (a real
    // typo) is processed anyway, as before qk-ixox, and the compiler reports the error. Only a
    // driver with spec IR is deferred: KSP runs another round only when this one wrote files, and
    // this round's flush writes that driver's spec types; without IR a driver is invalid at most
    // through references to its own not-yet-generated `generatedStep`, which never needs a second
    // round.
    private fun defer(clazz: KSClassDeclaration): Boolean {
        if (clazz.validate(enableNewFeatures = true)) return false
        return deferredOnce.add(clazz.qualifiedName!!.asString())
    }

    // Loads the driver's spec IR and generates its `<Module>Spec` types (qk-ixox); for @QuintRun
    // the caller also checks the driver's @QuintAction surface against it (qk-75ad).
    private fun loadIr(clazz: KSClassDeclaration, annotationShortName: String): QuintModuleIr? {
        val dir = irDir ?: return null
        val key = clazz.qualifiedName!!.asString()
        if (key in irByDriver) return irByDriver[key]

        val args = annotationArgs(clazz, annotationShortName)
        val spec = args["spec"] as? String
        val main = (args["main"] as? String)?.takeIf { it.isNotBlank() }
        @Suppress("UNCHECKED_CAST")
        val ignore = (args["ignore"] as? List<String>).orEmpty().filter { it.isNotBlank() }

        val module = when {
            spec == null -> null
            else -> try {
                loadQuintIrModule(dir, spec, main)
                    .also { if (it == null) logger.warn("quint-konnect: no quint IR found for spec \"$spec\" under $dir", clazz) }
            } catch (e: Exception) {
                logger.warn("quint-konnect: could not parse quint IR for spec \"$spec\": ${e.message}", clazz)
                null
            }
        }
        irByDriver[key] = module
        if (module != null) {
            specTypesGenerator.add(clazz, spec!!, module, ignore)
            val unknown = ignore.filterNot { it in module.variables }
            if (unknown.isNotEmpty()) {
                logger.error(
                    "quint-konnect: ignore names unknown state variable(s) ${unknown.sorted()} for " +
                        "spec \"$spec\"; known variables: ${module.variables.keys.sorted()}.",
                    clazz,
                )
            }
        }
        return module
    }

    private fun validateRunDriver(clazz: KSClassDeclaration, module: QuintModuleIr) {
        if (overridesNondetExtraction(clazz)) return
        val args = annotationArgs(clazz, "QuintRun")
        val init = (args["init"] as? String)?.takeIf { it.isNotBlank() } ?: "init"
        val step = (args["step"] as? String)?.takeIf { it.isNotBlank() } ?: "step"
        validateAgainstSpec(clazz, module, init, step, logger)
    }

    private fun annotationArgs(clazz: KSClassDeclaration, annotationShortName: String): Map<String, Any?> =
        clazz.annotations.first { it.shortName.asString() == annotationShortName }
            .arguments.associate { it.name!!.asString() to it.value }
}
