package io.github.mcbianconi.quintkonnect.ksp.generators

// Values pasted here (spec paths, seeds, test/action names) come from annotation
// arguments and can contain characters that are meaningful in a Kotlin string
// literal ("$", "\"", "\\"). Quote and escape them so the generated file compiles.
internal fun String.kotlinStringLiteral(): String = buildString {
    append('"')
    for (c in this@kotlinStringLiteral) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '$' -> append("\\$")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
    append('"')
}
