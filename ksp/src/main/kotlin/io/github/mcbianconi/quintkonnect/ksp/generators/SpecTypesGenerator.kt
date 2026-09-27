package io.github.mcbianconi.quintkonnect.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.mcbianconi.quintkonnect.ksp.ir.QuintModuleIr
import io.github.mcbianconi.quintkonnect.ksp.ir.QuintType

private val SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
private val SERIAL_NAME = ClassName("kotlinx.serialization", "SerialName")

// qk-ixox: writes `<Module>Spec.kt` into the driver's package, one `object <Module>Spec` holding
// a `@Serializable` mirror of every spec type the ITF decoder can read (skills/quint-konnect/
// references/types.md): the module's own non-generic typedefs, a `State` data class with every
// state variable, and whatever anonymous records/sums or imported typedefs those and the
// actions' nondet types reach. Everything is nested in the object so a spec type named like an
// implementation class in the same package (e.g. tictactoe's `Player`) doesn't clash.
//
// A type with no decodable Kotlin shape (a heterogeneous tuple, an uninterpreted type, ...) is left out along with everything containing it, and
// listed in one warning; a field is never dropped from a class that is generated.
//
// A driver whose `@QuintRun`/`@QuintTest` declares `ignore` (qk-ymex) also gets a
// `<Driver>State` class next to `State`: the same fields, minus the ignored variables. It's
// generated per driver, not by annotating `State` itself, since `State` (and its object) is
// shared by every driver of the same module (see `add` below) — one driver's ignore list has no
// business silently disabling another's comparison of the same field. Dropping the field
// entirely (rather than keeping it with a default, as `@QuintIgnore` does on a hand-written
// class) relies on `ItfValueDecoder` never validating for unknown ITF fields.
internal class SpecTypesGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {
    private class Pending(
        val spec: String,
        val module: QuintModuleIr,
        val drivers: MutableList<KSClassDeclaration>,
        val ignoreByDriver: MutableMap<KSClassDeclaration, List<String>> = mutableMapOf(),
    )

    // "<package>.<Object>" -> spec path, across rounds, so a later round doesn't rewrite a file.
    private val written = mutableMapOf<String, String>()
    private val pending = LinkedHashMap<String, Pending>()

    // Collects a driver's spec; `flush` writes one file per package and module. Every driver
    // sharing that file is one of its originating files: KSP's incremental mode reprocesses all
    // sources of a dirty output, so with only one recorded, deleting that driver would drop the
    // file the others still use. `ignore` (validated against `module.variables` by the caller)
    // is this driver's own `<Driver>State` projection request; empty means it only uses `State`.
    fun add(clazz: KSClassDeclaration, spec: String, module: QuintModuleIr, ignore: List<String> = emptyList()) {
        val packageName = clazz.packageName.asString()
        val objectName = pascalCase(module.name) + "Spec"
        val key = "$packageName.$objectName"
        val previous = written[key] ?: pending[key]?.spec
        when {
            previous == null -> pending[key] = Pending(spec, module, mutableListOf(clazz))
            previous != spec -> {
                logger.warn(
                    "quint-konnect: not generating $objectName for spec \"$spec\": $key is already " +
                        "generated from \"$previous\" (both declare a module named \"${module.name}\")",
                    clazz,
                )
                return
            }
            else -> pending[key]?.drivers?.add(clazz)
        }
        if (ignore.isNotEmpty()) pending[key]?.ignoreByDriver?.set(clazz, ignore)
    }

    fun flush() {
        for ((key, entry) in pending) {
            written[key] = entry.spec
            write(key.substringBeforeLast('.'), key.substringAfterLast('.'), entry)
        }
        pending.clear()
    }

    private fun write(packageName: String, objectName: String, entry: Pending) {
        val mapper = SpecTypeMapper(entry.module, ClassName(packageName, objectName))
        mapper.addOwnTypeDefs()
        mapper.addState()
        mapper.addNondets()
        for ((driver, ignore) in entry.ignoreByDriver) {
            mapper.addDriverState(driver.simpleName.asString(), ignore.toSet())
        }

        val wrapper = TypeSpec.objectBuilder(objectName)
            .addKdoc("Generated by quint-konnect from module `%L` of `%L`.", entry.module.name, entry.spec)
            .addTypes(mapper.types.values)
        entry.drivers.mapNotNull { it.containingFile }.distinct().forEach { wrapper.addOriginatingKSFile(it) }
        FileSpec.builder(packageName, objectName)
            .addType(wrapper.build())
            .build()
            .writeTo(codeGenerator, aggregating = false)

        if (mapper.skipped.isNotEmpty()) {
            logger.warn(
                "quint-konnect: $objectName leaves out spec types with no decodable Kotlin shape:\n" +
                    mapper.skipped.joinToString("\n") { "  - $it" },
                entry.drivers.first(),
            )
        }
    }
}

private class SpecTypeMapper(private val module: QuintModuleIr, private val wrapper: ClassName) {
    val types = LinkedHashMap<String, TypeSpec>()
    val skipped = mutableListOf<String>()

    private val stateName = if ("State" in module.typeDefs) "StateVars" else "State"
    private val takenNames = (module.typeDefs.keys + module.externalTypeDefs.keys + stateName).toMutableSet()
    private val namedTypes = mutableMapOf<String, ClassName>()
    private val anonymousTypes = mutableMapOf<QuintType, ClassName>()

    fun addOwnTypeDefs() {
        for ((name, type) in module.typeDefs) {
            if (name in module.typeDefParams || !type.isClassShaped()) continue
            val reason = unsupportedReason(type, mutableSetOf(name))
            if (reason != null) skipped += "type $name: $reason" else namedType(name)
        }
    }

    fun addState() {
        if (module.variables.isEmpty()) return
        val reasons = module.variables.mapNotNull { (name, type) ->
            unsupportedReason(type, mutableSetOf())?.let { "var $name: $it" }
        }
        if (reasons.isNotEmpty()) {
            skipped += "$stateName (the state record), because of:"
            skipped += reasons.map { "  $it" }
            return
        }
        val fields = module.variables.mapValues { (name, type) -> typeName(type, name) }
        types[stateName] = dataClass(wrapper.nestedClass(stateName), fields).build()
    }

    // qk-ymex: a `<driverSimpleName>State` class beside `State`, with `ignore`'s variables left
    // out entirely rather than kept with a default — the driver never has to supply a value for a
    // variable it doesn't model, and a variable whose type has no decodable Kotlin shape can be
    // projected away too (unlike `State`, which skips generating altogether if any variable is
    // unsupported). Only the *other* variables' shapes gate this class.
    fun addDriverState(driverSimpleName: String, ignore: Set<String>) {
        val included = module.variables.filterKeys { it !in ignore }
        val label = driverSimpleName + "State"
        val reasons = included.mapNotNull { (name, type) ->
            unsupportedReason(type, mutableSetOf())?.let { "var $name: $it" }
        }
        if (reasons.isNotEmpty()) {
            skipped += "$label (${driverSimpleName}'s state projection), because of:"
            skipped += reasons.map { "  $it" }
            return
        }
        val className = wrapper.nestedClass(freshName(label))
        val fields = included.mapValues { (name, type) -> typeName(type, name) }
        types[className.simpleName] = dataClass(className, fields).build()
    }

    fun addNondets() {
        val seen = mutableSetOf<String>()
        for (action in module.actions.values) {
            for (param in action.nondetParams) {
                if (!seen.add(param.name + "/" + param.type)) continue
                val reason = unsupportedReason(param.type, mutableSetOf())
                if (reason != null) skipped += "nondet ${param.name} (action ${action.name}): $reason"
                else typeName(param.type, param.name)
            }
        }
    }

    // Null when `type` maps onto a type ItfValue.decode can read; otherwise why not. `visiting`
    // holds the typedef names on the current path, so a recursive typedef counts as supported.
    private fun unsupportedReason(type: QuintType, visiting: MutableSet<String>): String? = when (type) {
        QuintType.BoolType, QuintType.IntType, QuintType.StrType -> null
        is QuintType.SetType -> unsupportedReason(type.element, visiting)
        is QuintType.ListType -> unsupportedReason(type.element, visiting)
        is QuintType.FunType -> unsupportedReason(type.arg, visiting) ?: unsupportedReason(type.res, visiting)
        is QuintType.TupleType -> when {
            type.elements.isEmpty() -> "the unit tuple () has no Kotlin field shape"
            type.elements.distinct().size > 1 -> "tuple ${render(type)} mixes element types; only a homogeneous tuple decodes (as List)"
            else -> unsupportedReason(type.elements.first(), visiting)
        }
        is QuintType.RecordType, is QuintType.SumType -> {
            val named = structuralName(type)?.takeIf { it !in visiting }
            val payload = optionPayload(type)
            when {
                named != null -> {
                    visiting.add(named)
                    unsupportedReason(typeDef(named), visiting).also { visiting.remove(named) }
                }
                payload != null && optionPayload(resolve(payload)) != null ->
                    "${render(type)} nests Option, which a nullable Kotlin type can't tell apart"
                payload != null -> unsupportedReason(payload, visiting)
                type is QuintType.RecordType -> type.fields.values.firstNotNullOfOrNull { unsupportedReason(it, visiting) }
                else -> (type as QuintType.SumType).variants.values.firstNotNullOfOrNull {
                    if (it.isUnit()) null else unsupportedReason(it, visiting)
                }
            }
        }
        is QuintType.ConstType -> {
            val name = typeDefName(type.name)
            when {
                name == null -> "${type.name} is an uninterpreted type or isn't declared in this file"
                name in module.typeDefParams -> "${type.name} is generic; only Option[T] is mapped"
                !visiting.add(name) -> null
                else -> unsupportedReason(typeDef(name), visiting).also { visiting.remove(name) }
            }
        }
        is QuintType.AppType -> when {
            !isOption(type) -> "${render(type)} applies a generic type; only Option[T] is mapped"
            optionPayload(resolve(type.args.single())) != null ->
                "${render(type)} nests Option, which a nullable Kotlin type can't tell apart"
            else -> unsupportedReason(type.args.single(), visiting)
        }
        is QuintType.VarType -> "type variable ${type.name} is unresolved"
        is QuintType.OperType -> "operator types aren't state or nondet values"
        is QuintType.UnknownType -> "quint IR type kind \"${type.kind}\" isn't recognized"
    }

    // Only called once unsupportedReason(type) returned null. `context` names anonymous
    // records/sums (a var, field, variant or nondet name).
    private fun typeName(type: QuintType, context: String): TypeName = when (type) {
        QuintType.BoolType -> BOOLEAN
        QuintType.IntType -> LONG
        QuintType.StrType -> STRING
        is QuintType.SetType -> SET.parameterizedBy(typeName(type.element, context))
        is QuintType.ListType -> LIST.parameterizedBy(typeName(type.element, context))
        is QuintType.FunType -> MAP.parameterizedBy(typeName(type.arg, context), typeName(type.res, context))
        is QuintType.TupleType -> LIST.parameterizedBy(typeName(type.elements.first(), context))
        is QuintType.RecordType, is QuintType.SumType -> {
            val name = structuralName(type)
            val payload = optionPayload(type)
            when {
                name != null -> namedType(name)
                payload != null -> typeName(payload, context).copy(nullable = true)
                else -> anonymousType(type, context)
            }
        }
        is QuintType.ConstType -> {
            val name = typeDefName(type.name)!!
            val def = typeDef(name)
            if (def.isClassShaped()) namedType(name) else typeName(def, context)
        }
        is QuintType.AppType -> typeName(type.args.single(), context).copy(nullable = true)
        else -> error("unsupported type reached typeName: $type")
    }

    private fun namedType(name: String): ClassName = namedTypes[name] ?: run {
        val className = wrapper.nestedClass(name)
        namedTypes[name] = className
        types[name] = declare(className, typeDef(name), name)
        className
    }

    private fun anonymousType(type: QuintType, context: String): ClassName {
        val key = expand(type, mutableSetOf())
        return anonymousTypes[key] ?: run {
            val className = wrapper.nestedClass(freshName(genericApplicationName(key) ?: pascalCase(context)))
            anonymousTypes[key] = className
            types[className.simpleName] = declare(className, type, context)
            className
        }
    }

    // quint expands an applied generic typedef (`Opt[Coin]`) into its body before KSP sees it,
    // so the application is recovered by matching `type` against each generic typedef's body and
    // named after the typedef and its arguments (`OptCoin`) rather than after its first use.
    private fun genericApplicationName(type: QuintType): String? {
        for ((name, params) in module.typeDefParams) {
            val body = expand(typeDef(name), mutableSetOf(name))
            val bindings = mutableMapOf<String, QuintType>()
            if (!unify(body, type, bindings)) continue
            val args = params.map { bindings[it]?.let(::argName) ?: return@map null }
            if (args.any { it == null }) continue
            return name + args.joinToString("")
        }
        return null
    }

    private fun unify(pattern: QuintType, type: QuintType, bindings: MutableMap<String, QuintType>): Boolean = when (pattern) {
        is QuintType.VarType -> bindings.getOrPut(pattern.name) { type } == type
        is QuintType.SetType -> type is QuintType.SetType && unify(pattern.element, type.element, bindings)
        is QuintType.ListType -> type is QuintType.ListType && unify(pattern.element, type.element, bindings)
        is QuintType.FunType -> type is QuintType.FunType && unify(pattern.arg, type.arg, bindings) && unify(pattern.res, type.res, bindings)
        is QuintType.TupleType -> type is QuintType.TupleType && pattern.elements.size == type.elements.size &&
            pattern.elements.zip(type.elements).all { (p, t) -> unify(p, t, bindings) }
        is QuintType.RecordType -> type is QuintType.RecordType && pattern.fields.keys == type.fields.keys &&
            pattern.fields.all { (k, p) -> unify(p, type.fields.getValue(k), bindings) }
        is QuintType.SumType -> type is QuintType.SumType && pattern.variants.keys == type.variants.keys &&
            pattern.variants.all { (k, p) -> unify(p, type.variants.getValue(k), bindings) }
        else -> pattern == type
    }

    private fun argName(type: QuintType): String? = when (type) {
        QuintType.BoolType -> "Bool"
        QuintType.IntType -> "Int"
        QuintType.StrType -> "Str"
        else -> structuralName(type) ?: genericApplicationName(type)
    }

    private fun declare(className: ClassName, type: QuintType, context: String): TypeSpec = when (type) {
        is QuintType.RecordType -> dataClass(className, type.fields.mapValues { (field, t) -> typeName(t, field) }).build()
        is QuintType.SumType -> sealedClass(className, type)
        else -> error("only records and sums get a class: $type ($context)")
    }

    private fun sealedClass(className: ClassName, type: QuintType.SumType): TypeSpec {
        val builder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.SEALED)
            .addAnnotation(SERIALIZABLE)
        for ((variant, payload) in type.variants) {
            val variantClass = className.nestedClass(variant)
            val variantSpec = if (payload.isUnit()) {
                TypeSpec.objectBuilder(variantClass).addModifiers(KModifier.DATA).addAnnotation(SERIALIZABLE)
            } else {
                val payloadName = className.simpleName + pascalCase(variant)
                dataClass(variantClass, mapOf("value" to typeName(payload, payloadName)))
            }
            builder.addType(
                variantSpec
                    .superclass(className)
                    .addAnnotation(AnnotationSpec.builder(SERIAL_NAME).addMember("%S", variant).build())
                    .build(),
            )
        }
        return builder.build()
    }

    private fun dataClass(className: ClassName, fields: Map<String, TypeName>): TypeSpec.Builder {
        val ctor = FunSpec.constructorBuilder()
        val builder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(SERIALIZABLE)
        for ((name, type) in fields) {
            ctor.addParameter(name, type)
            builder.addProperty(PropertySpec.builder(name, type).initializer("%N", name).build())
        }
        return builder.primaryConstructor(ctor.build())
    }

    private fun freshName(base: String): String {
        var name = base
        var suffix = 2
        while (!takenNames.add(name)) name = base + suffix++
        return name
    }

    // `import lib as L` makes a reference read `L::Coin`; the typedef itself is still `Coin`.
    private fun typeDefName(reference: String): String? {
        val bare = reference.substringAfterLast("::")
        return bare.takeIf { it in module.typeDefs || it in module.externalTypeDefs }
    }

    private fun typeDef(name: String): QuintType = module.typeDefs[name] ?: module.externalTypeDefs.getValue(name)

    private fun isOption(type: QuintType.AppType): Boolean =
        type.ctor.substringAfterLast("::") == "Option" && type.args.size == 1

    // quint writes `Option[T]` either as the application itself or, when the file declares
    // `type Option[a] = Some(a) | None`, already expanded into that sum; both decode as `T?`.
    private fun optionPayload(type: QuintType): QuintType? = when {
        type is QuintType.AppType && isOption(type) -> type.args.single()
        type is QuintType.SumType && type.variants.keys == setOf("Some", "None") &&
            type.variants.getValue("None").isUnit() -> type.variants.getValue("Some")
        else -> null
    }

    private fun resolve(type: QuintType): QuintType {
        var current = type
        val seen = mutableSetOf<String>()
        while (current is QuintType.ConstType) {
            val name = typeDefName(current.name)?.takeIf(seen::add) ?: return current
            current = typeDef(name)
        }
        return current
    }

    // Nondet types come from quint's inferred `types` table, where a typedef reference is
    // already expanded into its structure (`s: Shape` reads as the full sum); match those back
    // to the typedef's own class instead of generating a structural duplicate.
    private val typeDefsByStructure: Map<QuintType, String> by lazy {
        val out = LinkedHashMap<QuintType, String>()
        (module.typeDefs + module.externalTypeDefs.filterKeys { it !in module.typeDefs })
            .filter { (name, def) -> name !in module.typeDefParams && def.isClassShaped() }
            .forEach { (name, def) -> out.putIfAbsent(expand(def, mutableSetOf(name)), name) }
        out
    }

    private fun structuralName(type: QuintType): String? = typeDefsByStructure[expand(type, mutableSetOf())]

    private fun expand(type: QuintType, visiting: MutableSet<String>): QuintType = when (type) {
        is QuintType.ConstType -> {
            val name = typeDefName(type.name)
            if (name == null || name in module.typeDefParams || !visiting.add(name)) type
            else expand(typeDef(name), visiting).also { visiting.remove(name) }
        }
        is QuintType.SetType -> QuintType.SetType(expand(type.element, visiting))
        is QuintType.ListType -> QuintType.ListType(expand(type.element, visiting))
        is QuintType.FunType -> QuintType.FunType(expand(type.arg, visiting), expand(type.res, visiting))
        is QuintType.TupleType -> QuintType.TupleType(type.elements.map { expand(it, visiting) })
        is QuintType.RecordType -> QuintType.RecordType(type.fields.mapValues { expand(it.value, visiting) })
        is QuintType.SumType -> QuintType.SumType(type.variants.mapValues { expand(it.value, visiting) })
        is QuintType.AppType -> QuintType.AppType(type.ctor, type.args.map { expand(it, visiting) })
        else -> type
    }

    // Only records and sums get a class; an alias like `type Id = int` is inlined, since its ITF
    // value is the bare int, not a wrapper record.
    private fun QuintType.isClassShaped(): Boolean = this is QuintType.RecordType || this is QuintType.SumType

    private fun QuintType.isUnit(): Boolean = this is QuintType.TupleType && elements.isEmpty()

    private fun render(type: QuintType): String = when (type) {
        QuintType.BoolType -> "bool"
        QuintType.IntType -> "int"
        QuintType.StrType -> "str"
        is QuintType.ConstType -> type.name
        is QuintType.TupleType -> type.elements.joinToString(", ", "(", ")") { render(it) }
        is QuintType.AppType -> type.ctor + type.args.joinToString(", ", "[", "]") { render(it) }
        is QuintType.SetType -> "Set[${render(type.element)}]"
        is QuintType.ListType -> "List[${render(type.element)}]"
        else -> type.toString()
    }
}

internal fun pascalCase(name: String): String =
    name.split('_', '-', ':').filter { it.isNotEmpty() }.joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
