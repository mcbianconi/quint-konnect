package io.github.mcbianconi.quintkonnect.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import io.github.mcbianconi.quintkonnect.ksp.generators.ActionMember
import io.github.mcbianconi.quintkonnect.ksp.generators.findQuintAction
import io.github.mcbianconi.quintkonnect.ksp.ir.QuintModuleIr
import io.github.mcbianconi.quintkonnect.ksp.ir.QuintType
import io.github.mcbianconi.quintkonnect.ksp.ir.dispatchableActionNames

// qk-75ad: does `clazz` override `Driver.config()` to point `DriverConfig.nondetPath` at a
// hand-modeled sum type (docs/decisions/quint-test-needs-nondet-path.md)? That's not only a
// @QuintTest thing: example/.../sumtypes/VendingMachineDriver.kt is a @QuintRun driver that does
// this too, to keep `lastAction` out of state comparison. Either way, `Step.fromState` then reads
// actionTaken from that sum type's own tag instead of `mbt::actionTaken`
// (core/.../Step.kt's `extractFromSumType`), an entirely different namespace from
// module.actions that this check would misjudge -- so any config() override at all is treated as
// "can't statically tell which path this driver uses" and skips validation, rather than parsing
// the override body to look for a non-empty nondetPath.
internal fun overridesNondetExtraction(clazz: KSClassDeclaration): Boolean {
    val config = clazz.getAllFunctions()
        .firstOrNull { it.simpleName.asString() == "config" && it.parameters.isEmpty() }
        ?: return false
    return (config.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString() !=
        "io.github.mcbianconi.quintkonnect.Driver"
}

// Checks a @QuintRun driver's @QuintAction surface against its spec's typed IR: not run for
// @QuintTest or for a driver caught by overridesNondetExtraction above.
internal fun validateAgainstSpec(clazz: KSClassDeclaration, module: QuintModuleIr, init: String, step: String, logger: KSPLogger) {
    val actions = clazz.getAllFunctions()
        .mapNotNull { fn -> fn.findQuintAction()?.let { ActionMember(fn, it) } }
        .toList()

    // `mbt::nondetPicks` is scoped to the whole init/step call quint made, not to whichever
    // branch action `any {...}` picked (confirmed against a real `quint run --mbt` trace): a
    // nondet bound in a `let` right before a call, e.g. `any { nondet n = ...; add(n) }`
    // (fixture.qnt/dispatch.qnt's own style, also counter.qnt's), ends up on the entry point's own
    // nondetParams (here, "step"), never on "add"'s. A dispatched action's own nondetParams (bound
    // directly, or transitively via `if`/`match`, in its own body -- tictactoe.qnt's MoveX reaching
    // Win/Block this way) still take priority when a name collides between the two.
    val entryNondets = (module.actions[init]?.nondetParams.orEmpty() + module.actions[step]?.nondetParams.orEmpty())
        .associateBy { it.name }

    for (action in actions) {
        val actionIr = module.actions[action.actionName]
        if (actionIr == null) {
            logger.error(
                "quint-konnect: @QuintAction(\"${action.actionName}\") does not match any action " +
                    "in the spec. Known actions: ${module.actions.keys.sorted()}.",
                action.function,
            )
            continue
        }

        val nondetsByName = entryNondets + actionIr.nondetParams.associateBy { it.name }
        for (param in action.function.parameters) {
            val paramName = param.name?.asString() ?: continue
            val nondet = nondetsByName[paramName]
            if (nondet == null) {
                logger.error(
                    "quint-konnect: parameter \"$paramName\" of @QuintAction(\"${action.actionName}\") " +
                        "is not a nondet of that action in the spec. Known nondets: " +
                        "${nondetsByName.keys.sorted()}.",
                    param,
                )
                continue
            }

            val paramType = param.type.resolve()
            val kotlinClass = classifyKotlin(paramType)
            val quintClass = classifyQuint(nondet.type, module.typeDefs, mutableSetOf())
            if (kotlinClass != null && quintClass != null && kotlinClass != quintClass) {
                logger.error(
                    "quint-konnect: parameter \"$paramName\" of @QuintAction(\"${action.actionName}\") " +
                        "has type $paramType, which doesn't decode from the spec's nondet type " +
                        "(${nondet.type}). See skills/quint-konnect/references/types.md.",
                    param,
                )
            }
        }
    }

    val annotatedNames = actions.map { it.actionName }.toSet()
    val dispatchable = module.dispatchableActionNames(listOf(init, step))
    for (name in dispatchable.sorted()) {
        if (name !in annotatedNames) {
            logger.warn(
                "quint-konnect: spec action \"$name\" is reachable at runtime but has no " +
                    "@QuintAction; a trace reaching it will fail.",
                clazz,
            )
        }
    }
}

// A coarse classification both a KSType (a driver's declared parameter type) and a QuintType (an
// IR-derived nondet type) reduce to, per skills/quint-konnect/references/types.md. Anything that
// doesn't reduce to one of these (a record, a sum type, an unresolved alias, a type parameter, a
// user-defined class) classifies as null: qk-75ad only flags a *clear* mismatch (e.g. int vs
// String), never a type it can't judge -- generating and checking the exact record/sum-type shape
// is qk-ixox's job, not this one's.
private enum class TypeClass { INT, BOOL, STR, SET, LIST, MAP }

private fun classifyKotlin(type: KSType): TypeClass? {
    if (type.isMarkedNullable) return classifyKotlin(type.makeNotNullable())
    val declaration = type.declaration
    if (declaration is KSTypeAlias) return classifyKotlin(declaration.type.resolve())
    if (declaration is KSTypeParameter) return null
    return when (declaration.qualifiedName?.asString()) {
        "kotlin.Boolean" -> TypeClass.BOOL
        "kotlin.String" -> TypeClass.STR
        "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long", "java.math.BigInteger" -> TypeClass.INT
        "kotlin.collections.Set" -> TypeClass.SET
        "kotlin.collections.List" -> TypeClass.LIST
        "kotlin.collections.Map" -> TypeClass.MAP
        else -> null
    }
}

// `visitedConstNames` guards against a self-referential typedef (shouldn't happen after a
// successful typecheck, but a malformed/partial IR shouldn't infinite-loop here either).
private fun classifyQuint(type: QuintType, typeDefs: Map<String, QuintType>, visitedConstNames: MutableSet<String>): TypeClass? = when (type) {
    is QuintType.BoolType -> TypeClass.BOOL

    is QuintType.IntType -> TypeClass.INT

    is QuintType.StrType -> TypeClass.STR

    is QuintType.SetType -> TypeClass.SET

    is QuintType.ListType -> TypeClass.LIST

    is QuintType.TupleType -> TypeClass.LIST

    is QuintType.FunType -> TypeClass.MAP

    is QuintType.ConstType ->
        typeDefs[type.name]?.takeIf { visitedConstNames.add(type.name) }?.let { classifyQuint(it, typeDefs, visitedConstNames) }

    is QuintType.RecordType, is QuintType.SumType, is QuintType.VarType, is QuintType.OperType,
    is QuintType.AppType, is QuintType.UnknownType,
    -> null
}
