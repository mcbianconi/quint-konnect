package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import io.github.mcbianconi.quintkonnect.ksp.ir.IR_DIR_OPTION_NAME
import java.io.File

public class QuintKonnectProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        QuintKonnectProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options[IR_DIR_OPTION_NAME]?.takeIf { it.isNotBlank() }?.let(::File),
        )
}
