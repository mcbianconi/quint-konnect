package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration

// qk-9lsz: `Runner.runTest`'s generated `driverFactory = { X() }` call needs a constructor
// callable with no arguments; report that at compile time instead of leaving it as a confusing
// error in KSP-generated code.
internal fun validateNoArgConstructor(clazz: KSClassDeclaration, logger: KSPLogger): Boolean {
    if (hasPublicNoArgConstructor(clazz)) return false
    logger.error(
        "${clazz.simpleName.asString()} must have a public no-arg constructor: the generated " +
            "test class instantiates the driver with ${clazz.simpleName.asString()}().",
        clazz,
    )
    return true
}

private fun hasPublicNoArgConstructor(clazz: KSClassDeclaration): Boolean {
    if (clazz.classKind != ClassKind.CLASS || clazz.isAbstract()) return false
    val constructors = listOfNotNull(clazz.primaryConstructor) + clazz.getConstructors()
    return constructors.any { ctor -> ctor.isPublic() && ctor.parameters.all { it.hasDefault } }
}
