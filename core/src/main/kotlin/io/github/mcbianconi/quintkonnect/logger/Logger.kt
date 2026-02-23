package io.github.mcbianconi.quintkonnect.logger

private val verbosity: Int =
    System.getenv("QUINT_VERBOSE")?.toIntOrNull() ?: 0

private fun indent(text: String, spaces: Int = 3): String {
    val prefix = " ".repeat(spaces)
    return text.lines().joinToString("\n") { prefix + it }
}

private const val BOLD   = "\u001B[1m"
private const val GREEN  = "\u001B[32m"
private const val RED    = "\u001B[31m"
private const val DIM    = "\u001B[2m"
private const val WHITE  = "\u001B[97m"
private const val RESET  = "\u001B[0m"

object Logger {
    fun title(msg: String) = System.err.println("$BOLD== $msg$RESET")

    fun info(msg: String) = System.err.println(indent(msg))

    fun success(msg: String) = System.err.println("$BOLD$GREEN${indent(msg)}$RESET")

    fun error(msg: String) = System.err.println("$BOLD$RED${indent(msg)}$RESET")

    fun trace(level: Int, msg: String) {
        if (verbosity >= level) {
            System.err.println("$DIM$WHITE${indent(msg)}$RESET")
        }
    }
}
