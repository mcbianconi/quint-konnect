package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import io.github.mcbianconi.quintkonnect.ksp.generators.QuintRunTestGenerator
import io.github.mcbianconi.quintkonnect.ksp.generators.QuintTestTestGenerator
import io.github.mcbianconi.quintkonnect.ksp.generators.StepMethodGenerator

internal class QuintKonnectProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
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
            }

        resolver.getSymbolsWithAnnotation(quintTestFqn)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { clazz ->
                if (validateNoArgConstructor(clazz, logger)) return@forEach
                if (validateQuintStateType(clazz, resolver, logger)) return@forEach
                StepMethodGenerator(codeGenerator, logger).generate(clazz, resolver)
                QuintTestTestGenerator(codeGenerator, logger).generate(clazz)
            }

        return emptyList()
    }
}
