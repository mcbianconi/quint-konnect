package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Variance

// qk-9lsz: the generated `<Driver>QuintSuite`'s `driverFactory = { X() }` call needs a constructor
// callable with no arguments; report that at compile time instead of leaving it as a confusing
// error in KSP-generated code.
internal fun validateNoArgConstructor(clazz: KSClassDeclaration, logger: KSPLogger): Boolean {
    if (hasPublicNoArgConstructor(clazz)) return false
    logger.error(
        "${clazz.simpleName.asString()} must have a public no-arg constructor: the generated " +
            "QuintSuite instantiates the driver with ${clazz.simpleName.asString()}().",
        clazz,
    )
    return true
}

private fun hasPublicNoArgConstructor(clazz: KSClassDeclaration): Boolean {
    if (clazz.classKind != ClassKind.CLASS || clazz.isAbstract()) return false
    val constructors = listOfNotNull(clazz.primaryConstructor) + clazz.getConstructors()
    return constructors.any { ctor -> ctor.isPublic() && ctor.parameters.all { it.hasDefault } }
}

private const val DRIVER_FQN = "io.github.mcbianconi.quintkonnect.Driver"
private const val STATE_FQN = "io.github.mcbianconi.quintkonnect.State"

// qk-hpzp: `ReplayRunner.runTest<D>` infers `D` as the concrete driver class (`driverFactory =
// { X() }`) and then casts `driver.quintState()` to `State<D>` unchecked, since `Driver.quintState()`
// only declares the erased `State<*>`. That cast is sound exactly when the state a driver returns
// is a `State` of the driver's own class or one of its supertypes; catch a mismatch here, at
// compile time, instead of an unchecked-cast failure at replay time.
internal fun validateQuintStateType(clazz: KSClassDeclaration, resolver: Resolver, logger: KSPLogger): Boolean {
    val quintState = clazz.getAllFunctions()
        .firstOrNull { it.simpleName.asString() == "quintState" && it.parameters.isEmpty() }
        ?: return false

    // The inherited default (`State.disabled<Driver>()`) is always sound; only a driver's own
    // override (directly or via an intermediate base class) needs checking.
    if ((quintState.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString() == DRIVER_FQN) {
        return false
    }

    val returnType = quintState.returnType?.resolve() ?: return false
    if (returnType.isError) return false

    val stateDecl = resolver.getClassDeclarationByName(STATE_FQN) ?: return false
    val driverArg = resolver.getTypeArgument(
        resolver.createKSTypeReferenceFromKSType(clazz.asStarProjectedType()),
        Variance.CONTRAVARIANT,
    )
    val expectedType = stateDecl.asType(listOf(driverArg))
    if (expectedType.isAssignableFrom(returnType)) return false

    logger.error(
        "${clazz.simpleName.asString()}.quintState() returns $returnType, which is not a " +
            "State<${clazz.simpleName.asString()}> (or a State of one of its supertypes). Declare " +
            "quintState()'s return type explicitly, e.g. \"State<${clazz.simpleName.asString()}>\", " +
            "so the compiler can check it against the driver it's returned from.",
        quintState,
    )
    return true
}
