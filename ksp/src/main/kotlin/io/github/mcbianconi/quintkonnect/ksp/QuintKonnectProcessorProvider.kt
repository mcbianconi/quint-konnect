package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import io.github.mcbianconi.quintkonnect.ksp.ir.IR_DIR_OPTION_NAME
import java.io.File

// Selects the generated test-framework adapter (ksp/.../generators/QuintSuiteGenerator.kt):
// "junit" (also the default when the option is absent or blank) generates the JUnit class
// alongside the runner-neutral suite; "none" generates only the suite. Any other value is a
// compile error (see AdapterOption.resolve).
internal const val ADAPTER_OPTION_NAME: String = "quintkonnect.adapter"

internal enum class AdapterOption {
    JUNIT,
    NONE,
    ;

    internal companion object {
        fun resolve(raw: String?): AdapterOption? = when (raw?.trim().orEmpty()) {
            "", "junit" -> JUNIT
            "none" -> NONE
            else -> null
        }
    }
}

public class QuintKonnectProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        QuintKonnectProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options[IR_DIR_OPTION_NAME]?.takeIf { it.isNotBlank() }?.let(::File),
            environment.options[ADAPTER_OPTION_NAME],
        )
}
