package io.github.mcbianconi.quintkonnect.ksp.ir

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File

// Mirrors QUINT_IR_DIR_OPTION in gradle-plugin/.../QuintIrTask.kt: the KSP processor option
// QuintIrTask's output directory is passed under (internal visibility is per-module, so this
// can't just reference gradle-plugin's constant).
internal const val IR_DIR_OPTION_NAME: String = "quintkonnect.irDir"

// A module from quint's typed IR (`quint typecheck --out`, quint 0.32.0), scoped to what
// qk-75ad (validate @QuintAction names/params against the spec) and qk-ixox (generate Kotlin
// types from spec types) need: an action's transitively reachable nondet variables and their
// types, state variables' types, and named type definitions (records, sum types).
internal data class QuintModuleIr(
    val name: String,
    val actions: Map<String, QuintActionIr>,
    val variables: Map<String, QuintType>,
    val typeDefs: Map<String, QuintType>,
    // Raw per-action-def expr and the document's name-resolution table, kept only for
    // dispatchableActionNames (qk-75ad): unlike nondetParams, telling apart a directly
    // dispatchable action (reached through `any {...}`, quint's own opcode "actionAny") from a
    // helper one (reached through `if`/`match`) needs the expression shape itself, not just the
    // transitively-collected nondets.
    val actionExprs: Map<String, JsonObject>,
    val resolutionTable: JsonObject,
    // A generic typedef's own type parameter names (`type Opt[a] = ...` -> ["a"]); absent for a
    // non-generic one.
    val typeDefParams: Map<String, List<String>> = emptyMap(),
    // Typedefs declared in the same file's other modules (e.g. an imported `lib` module), for
    // resolving a ConstType this module doesn't declare itself.
    val externalTypeDefs: Map<String, QuintType> = emptyMap(),
)

internal data class QuintActionIr(val name: String, val nondetParams: List<QuintNondetParam>)

internal data class QuintNondetParam(val name: String, val type: QuintType)

// Mirrors QuintType (quint's dist/src/ir/quintTypes.d.ts, @informalsystems/quint npm package):
// bool/int/str/const/var/set/list/fun/oper/tup/rec/sum/app. See docs/decisions/quint-ir-source.md
// for why field names here track quint's own IR instead of a Kotlin-shaped model.
internal sealed class QuintType {
    internal object BoolType : QuintType()
    internal object IntType : QuintType()
    internal object StrType : QuintType()

    // A reference to a `type X = ...` alias/typedef, or an uninterpreted type: resolve against
    // QuintModuleIr.typeDefs.
    internal data class ConstType(val name: String) : QuintType()

    // An unresolved type variable: shouldn't normally appear once a spec typechecks, kept so a
    // malformed/partial IR doesn't crash parsing.
    internal data class VarType(val name: String) : QuintType()
    internal data class SetType(val element: QuintType) : QuintType()
    internal data class ListType(val element: QuintType) : QuintType()
    internal data class FunType(val arg: QuintType, val res: QuintType) : QuintType()
    internal data class OperType(val args: List<QuintType>, val res: QuintType) : QuintType()

    // Field names are "0", "1", ... in declaration order; unit is a TupleType(emptyList()).
    internal data class TupleType(val elements: List<QuintType>) : QuintType()
    internal data class RecordType(val fields: Map<String, QuintType>) : QuintType()

    // `type P = X | O(int)`: variant name -> its payload type (TupleType(emptyList()) for a
    // unit variant like `X`, RecordType for `O({ n: int })`).
    internal data class SumType(val variants: Map<String, QuintType>) : QuintType()

    // A generic type alias applied to arguments, e.g. `Foo[int]` for `type Foo[a] = ...`.
    internal data class AppType(val ctor: String, val args: List<QuintType>) : QuintType()

    // A type `kind` this parser doesn't recognize (e.g. a future quint IR addition): kept instead
    // of failing the whole parse, since qk-8i6m doesn't validate anything yet.
    internal data class UnknownType(val kind: String) : QuintType()
}

// Parses every module in a `quint typecheck --out <file>` JSON document. Throws if the document
// itself carries typecheck errors (QuintIrTask already fails the build in that case; this is a
// second line of defense for a caller that reads a stale/hand-edited file directly).
internal fun parseQuintIr(json: String): List<QuintModuleIr> {
    val root = Json.parseToJsonElement(json).jsonObject
    val errors = root["errors"]?.jsonArray.orEmpty()
    require(errors.isEmpty()) {
        "quint IR has typecheck errors:\n" + errors.joinToString("\n") {
            it.jsonObject["explanation"]?.jsonPrimitive?.contentOrNull ?: it.toString()
        }
    }

    val types = root["types"]?.jsonObject ?: JsonObject(emptyMap())
    val table = root["table"]?.jsonObject ?: JsonObject(emptyMap())
    val modules = root["modules"]?.jsonArray.orEmpty()
    val parsed = modules.map { parseModule(it.jsonObject, types, table) }
    return parsed.map { module ->
        val external = LinkedHashMap<String, QuintType>()
        parsed.filter { it !== module }.forEach { other -> other.typeDefs.forEach { (k, v) -> external.putIfAbsent(k, v) } }
        module.copy(externalTypeDefs = external - module.typeDefs.keys)
    }
}

// Loads the IR for one driver's spec: `<irDir>/<specPath>.json`, matching QuintIrTask's output
// naming (specPath relative to the project directory, same as `@QuintRun`/`@QuintTest`'s `spec`).
// Returns null when the file doesn't exist (spec not covered by quintKonnect.quintIrSpecs) or the
// requested main module isn't found, rather than failing the build: a miss stays a warning even
// after qk-75ad (QuintKonnectProcessor.loadIr), since quintIrSpecs can legitimately leave a spec
// out; qk-75ad only turns a *found* module's own name/param mismatches into errors.
internal fun loadQuintIrModule(irDir: File, specPath: String, main: String?): QuintModuleIr? {
    val irFile = File(irDir, "$specPath.json")
    if (!irFile.isFile) return null
    val modules = parseQuintIr(irFile.readText())
    if (modules.isEmpty()) return null
    if (!main.isNullOrBlank()) return modules.firstOrNull { it.name == main }
    if (modules.size == 1) return modules.single()
    // Multiple modules and no explicit `main`: quint itself would derive a default name from the
    // file name (RunConfig/TestConfig just omit `--main` and let it do so, core/.../RunConfig.kt);
    // reimplementing that here would need to guess at unpublished normalization rules, so this
    // falls back to the last module instead, which is quint's own convention for "the module the
    // file is named after" when a spec both declares and imports library modules.
    return modules.lastOrNull()
}

private fun parseModule(module: JsonObject, types: JsonObject, table: JsonObject): QuintModuleIr {
    val declarations = module["declarations"]?.jsonArray.orEmpty().map { it.jsonObject }

    val variables = declarations
        .filter { it["kind"]?.jsonPrimitive?.contentOrNull == "var" }
        .associate { decl ->
            decl.name() to parseType(decl.getObject("typeAnnotation"))
        }

    val typeDefDecls = declarations.filter { it["kind"]?.jsonPrimitive?.contentOrNull == "typedef" }
    val typeDefs = typeDefDecls
        .mapNotNull { decl -> decl["type"]?.jsonObject?.let { decl.name() to parseType(it) } }
        .toMap()
    val typeDefParams = typeDefDecls
        .mapNotNull { decl ->
            decl["params"]?.jsonArray?.map { it.jsonPrimitive.content }?.takeIf { it.isNotEmpty() }?.let { decl.name() to it }
        }
        .toMap()

    val actionDefs = declarations.filter {
        it["kind"]?.jsonPrimitive?.contentOrNull == "def" &&
            it["qualifier"]?.jsonPrimitive?.contentOrNull == "action"
    }
    val actions = actionDefs.associate { decl ->
        val nondets = LinkedHashMap<String, QuintNondetParam>()
        collectNondets(decl.getObject("expr"), types, table, mutableSetOf(decl.id()), nondets)
        decl.name() to QuintActionIr(decl.name(), nondets.values.toList())
    }
    val actionExprs = actionDefs.associate { decl -> decl.name() to decl.getObject("expr") }

    return QuintModuleIr(module.name(), actions, variables, typeDefs, actionExprs, table, typeDefParams)
}

// The set of action names `mbt::actionTaken` can actually record for a driver whose init/step
// entry points are `entryNames` (default "init"/"step", or @QuintRun's own init/step overrides):
// an entry point whose own expr is a direct `any {...}` (opcode "actionAny", confirmed against
// quint 0.32.0's IR and its own `--mbt` output in docs/decisions/quint-ir-source.md) contributes
// its resolved branches instead of its own name, recursively (a branch can itself be another
// `any {...}`); anything else (e.g. a plain `all {...}`, as tictactoe.qnt's `init`) contributes
// its own name, since that's what `--mbt` records when there's no further any-dispatch. A branch
// bound through a `let` (e.g. `any { nondet n = ...; add(n) }`, quint-konnect's fixture.qnt/probe
// spec) is unwrapped down to the call it lets into first. An entry name absent from the module
// (a bad @QuintRun override) contributes nothing, rather than guessing.
internal fun QuintModuleIr.dispatchableActionNames(entryNames: Collection<String>): Set<String> {
    val visited = mutableSetOf<String>()
    val out = mutableSetOf<String>()
    entryNames.forEach { collectDispatchNames(it, actionExprs, resolutionTable, visited, out) }
    return out
}

private fun collectDispatchNames(
    name: String,
    actionExprs: Map<String, JsonObject>,
    table: JsonObject,
    visited: MutableSet<String>,
    out: MutableSet<String>,
) {
    if (!visited.add(name)) return
    val expr = actionExprs[name] ?: return
    if (expr["kind"]?.jsonPrimitive?.contentOrNull == "app" && expr["opcode"]?.jsonPrimitive?.contentOrNull == "actionAny") {
        for (arg in expr["args"]?.jsonArray.orEmpty()) {
            val resolved = resolveCallable(unwrapLet(arg.jsonObject), table) ?: continue
            collectDispatchNames(resolved.name(), actionExprs, table, visited, out)
        }
    } else {
        out.add(name)
    }
}

private fun unwrapLet(expr: JsonObject): JsonObject {
    var current = expr
    while (current["kind"]?.jsonPrimitive?.contentOrNull == "let") {
        current = current.getObject("expr")
    }
    return current
}

// Qualifiers whose own def can textually contain a `nondet` binding, or itself be a chain to one
// (val/pureval/puredef helpers called from an action, e.g. tictactoe.qnt's `MoveX` reaching
// `StartInCorner`/`Win`/`Block`/... through `if`-branches and zero-arg name references).
private val CALLABLE_QUALIFIERS = setOf("action", "def", "val", "pureval", "puredef", "nondet", "run", "temporal")

// Walks an action's (or a def it calls into) expression tree, collecting every `nondet` binding
// reachable from it: both ones bound directly in its own body (`nondet x = ...`) and ones bound in
// another top-level def it references by name or application (resolved through `table`, quint's
// own name-resolution map), recursively. `visited` dedupes by the resolved def's own id, so a
// shared helper isn't walked twice and mutual/self recursion terminates.
private fun collectNondets(
    expr: JsonObject,
    types: JsonObject,
    table: JsonObject,
    visited: MutableSet<Long>,
    out: LinkedHashMap<String, QuintNondetParam>,
) {
    when (expr["kind"]?.jsonPrimitive?.contentOrNull) {
        "let" -> {
            val opdef = expr.getObject("opdef")
            if (opdef["qualifier"]?.jsonPrimitive?.contentOrNull == "nondet") {
                val name = opdef.name()
                val type = types[opdef.id().toString()]?.jsonObject?.get("type")?.jsonObject
                    ?.let(::parseType) ?: QuintType.UnknownType("nondet:$name")
                out.putIfAbsent(name, QuintNondetParam(name, type))
            }
            collectNondets(opdef.getObject("expr"), types, table, visited, out)
            collectNondets(expr.getObject("expr"), types, table, visited, out)
        }
        "lambda" -> collectNondets(expr.getObject("expr"), types, table, visited, out)
        "app" -> {
            resolveCallable(expr, table)?.let { recurseIntoDef(it, types, table, visited, out) }
            expr["args"]?.jsonArray.orEmpty().forEach { collectNondets(it.jsonObject, types, table, visited, out) }
        }
        "name" -> resolveCallable(expr, table)?.let { recurseIntoDef(it, types, table, visited, out) }
        else -> {}
    }
}

// `table` maps a name/app reference's own id to what it resolves to; that's absent for builtin
// opcodes (Set, filter, oneOf, actionAll, ...), so its presence alone tells apart a user-defined
// operator call from a builtin one.
private fun resolveCallable(expr: JsonObject, table: JsonObject): JsonObject? {
    val id = expr["id"]?.jsonPrimitive?.long ?: return null
    val resolved = table[id.toString()]?.jsonObject ?: return null
    if (resolved["kind"]?.jsonPrimitive?.contentOrNull != "def") return null
    if (resolved["qualifier"]?.jsonPrimitive?.contentOrNull !in CALLABLE_QUALIFIERS) return null
    if (resolved["expr"] == null) return null
    return resolved
}

private fun recurseIntoDef(
    def: JsonObject,
    types: JsonObject,
    table: JsonObject,
    visited: MutableSet<Long>,
    out: LinkedHashMap<String, QuintNondetParam>,
) {
    if (!visited.add(def.id())) return
    collectNondets(def.getObject("expr"), types, table, visited, out)
}

private fun parseType(type: JsonObject): QuintType = when (type["kind"]?.jsonPrimitive?.contentOrNull) {
    "bool" -> QuintType.BoolType
    "int" -> QuintType.IntType
    "str" -> QuintType.StrType
    "const" -> QuintType.ConstType(type.name())
    "var" -> QuintType.VarType(type.name())
    "set" -> QuintType.SetType(parseType(type.getObject("elem")))
    "list" -> QuintType.ListType(parseType(type.getObject("elem")))
    "fun" -> QuintType.FunType(parseType(type.getObject("arg")), parseType(type.getObject("res")))
    "oper" -> QuintType.OperType(
        type["args"]?.jsonArray.orEmpty().map { parseType(it.jsonObject) },
        parseType(type.getObject("res")),
    )
    "tup" -> QuintType.TupleType(parseRow(type.getObject("fields")).values.toList())
    "rec" -> QuintType.RecordType(parseRow(type.getObject("fields")))
    "sum" -> QuintType.SumType(parseRow(type.getObject("fields")))
    "app" -> QuintType.AppType(
        type.getObject("ctor").name(),
        type["args"]?.jsonArray.orEmpty().map { parseType(it.jsonObject) },
    )
    else -> QuintType.UnknownType(type["kind"]?.jsonPrimitive?.contentOrNull ?: "?")
}

// A Row is `{kind:"row", fields:[{fieldName,fieldType}], other}` (fixed or open), `{kind:"var"}`
// (a row variable) or `{kind:"empty"}"`. Only "row" carries fields; the others describe a
// polymorphic tail that shouldn't remain after a successful typecheck of a concrete tup/rec/sum.
private fun parseRow(row: JsonObject): Map<String, QuintType> {
    if (row["kind"]?.jsonPrimitive?.contentOrNull != "row") return emptyMap()
    val result = LinkedHashMap<String, QuintType>()
    for (field in row["fields"]?.jsonArray.orEmpty()) {
        val f = field.jsonObject
        result[f["fieldName"]!!.jsonPrimitive.content] = parseType(f.getObject("fieldType"))
    }
    return result
}

private fun JsonObject.name(): String = this["name"]!!.jsonPrimitive.content
private fun JsonObject.id(): Long = this["id"]!!.jsonPrimitive.long
private fun JsonObject.getObject(key: String): JsonObject = this[key]!!.jsonObject
