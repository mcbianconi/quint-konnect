@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.BigIntegerSerializer
import io.github.mcbianconi.quintkonnect.annotations.QuintIgnore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.math.BigInteger

/**
 * A generic value tree built from a `@Serializable` value's own [SerialDescriptor] (see
 * [buildDiffTree]), used to diff a spec-decoded and a driver-extracted [TypedState] value field by
 * field (see [buildFieldDiff]).
 */
internal sealed class DiffValue {
    data class Leaf(val text: String) : DiffValue()
    data class Seq(val items: List<DiffValue>, val ordered: Boolean) : DiffValue()
    data class MapNode(val entries: List<Pair<DiffValue, DiffValue>>) : DiffValue()
    data class Struct(val fields: LinkedHashMap<String, DiffValue>) : DiffValue()
}

internal fun DiffValue.render(): String = when (this) {
    is DiffValue.Leaf -> text
    is DiffValue.Seq -> if (ordered) {
        "(${items.joinToString(", ") { it.render() }})"
    } else {
        "Set(${items.joinToString(", ") { it.render() }})"
    }
    is DiffValue.MapNode -> "Map(${entries.joinToString(", ") { (k, v) -> "${k.render()} -> ${v.render()}" }})"
    is DiffValue.Struct -> "{ ${fields.entries.joinToString(", ") { (k, v) -> "$k: ${v.render()}" }} }"
}

private val diffSerializersModule: SerializersModule = SerializersModule {
    contextual(BigInteger::class, BigIntegerSerializer)
}

/** Builds a [DiffValue] tree for [value] using [serializer], walking its [SerialDescriptor]. */
internal fun <T> buildDiffTree(serializer: KSerializer<T>, value: T): DiffValue {
    var result: DiffValue = DiffValue.Leaf("null")
    val encoder = ValueSlotEncoder(diffSerializersModule) { result = it }
    encoder.encodeSerializableValue(serializer, value)
    return result
}

/**
 * Overrides the default comparison at a given field path, given both sides' rendered [DiffValue]
 * (see [DiffValue.render]): `null` falls through to the default structural comparison, `true`/`false`
 * treats the whole subtree at that path as equal/unequal without recursing further. See
 * [TypedState.compareField].
 */
internal typealias FieldComparator = (path: String, spec: String, impl: String) -> Boolean?

private val noopComparator: FieldComparator = { _, _, _ -> null }

/**
 * Builds a field-level diff between [spec] and [impl] (both decoded/extracted via [serializer]) as
 * `path: spec=..., impl=...` lines, reporting missing/extra keys or elements for collections and
 * maps instead of a value mismatch. Fields whose [SerialDescriptor] element is annotated
 * `@QuintIgnore` never appear in the tree walked here (see [ClassTreeEncoder.place]), so they never
 * take part in the diff. [compareField] can override the comparison at any path.
 */
internal fun <S> buildFieldDiff(
    serializer: KSerializer<S>,
    spec: S,
    impl: S,
    compareField: FieldComparator = noopComparator,
): List<String> {
    val out = mutableListOf<String>()
    diffValues("", buildDiffTree(serializer, spec), buildDiffTree(serializer, impl), compareField, out)
    return out
}

private fun String.field(name: String): String = if (isEmpty()) name else "$this.$name"
private fun String.index(i: Int): String = "$this[$i]"
private fun String.orRoot(): String = ifEmpty { "<root>" }

private fun diffValues(
    path: String,
    spec: DiffValue,
    impl: DiffValue,
    compareField: FieldComparator,
    out: MutableList<String>,
) {
    val override = compareField(path, spec.render(), impl.render())
    if (override != null) {
        if (!override) out += "${path.orRoot()}: spec=${spec.render()}, impl=${impl.render()}"
        return
    }

    when {
        spec is DiffValue.Leaf && impl is DiffValue.Leaf -> {
            if (spec.text != impl.text) out += "${path.orRoot()}: spec=${spec.text}, impl=${impl.text}"
        }

        spec is DiffValue.Struct && impl is DiffValue.Struct -> {
            for ((name, specChild) in spec.fields) {
                val implChild = impl.fields[name]
                if (implChild == null) {
                    out += "${path.field(name)}: missing in impl (spec=${specChild.render()})"
                } else {
                    diffValues(path.field(name), specChild, implChild, compareField, out)
                }
            }
            for (name in impl.fields.keys - spec.fields.keys) {
                out += "${path.field(name)}: extra in impl (impl=${impl.fields.getValue(name).render()})"
            }
        }

        spec is DiffValue.Seq && impl is DiffValue.Seq && spec.ordered && impl.ordered -> {
            val size = maxOf(spec.items.size, impl.items.size)
            for (i in 0 until size) {
                val s = spec.items.getOrNull(i)
                val d = impl.items.getOrNull(i)
                when {
                    s == null -> out += "${path.index(i)}: extra in impl (impl=${d!!.render()})"
                    d == null -> out += "${path.index(i)}: missing in impl (spec=${s.render()})"
                    else -> diffValues(path.index(i), s, d, compareField, out)
                }
            }
        }

        spec is DiffValue.Seq && impl is DiffValue.Seq -> {
            val specTexts = spec.items.map { it.render() }.toSet()
            val implTexts = impl.items.map { it.render() }.toSet()
            for (text in specTexts - implTexts) out += "${path.orRoot()}: missing in impl ($text)"
            for (text in implTexts - specTexts) out += "${path.orRoot()}: extra in impl ($text)"
        }

        spec is DiffValue.MapNode && impl is DiffValue.MapNode -> {
            val specEntries = spec.entries.associate { (k, v) -> k.render() to v }
            val implEntries = impl.entries.associate { (k, v) -> k.render() to v }
            for ((key, specChild) in specEntries) {
                val implChild = implEntries[key]
                if (implChild == null) {
                    out += "${path.field(key)}: missing in impl (spec=${specChild.render()})"
                } else {
                    diffValues(path.field(key), specChild, implChild, compareField, out)
                }
            }
            for (key in implEntries.keys - specEntries.keys) {
                out += "${path.field(key)}: extra in impl (impl=${implEntries.getValue(key).render()})"
            }
        }

        else -> if (spec.render() != impl.render()) {
            out += "${path.orRoot()}: spec=${spec.render()}, impl=${impl.render()}"
        }
    }
}

/**
 * An [Encoder] slot for a single value: whichever `encode*` method the value's serializer calls
 * determines [onResult]'s [DiffValue]. [BigIntegerSerializer] is special-cased since it only
 * supports [kotlinx.serialization.json.JsonEncoder] (see its KDoc).
 */
private class ValueSlotEncoder(
    override val serializersModule: SerializersModule,
    private val onResult: (DiffValue) -> Unit,
) : Encoder {

    override fun encodeBoolean(value: Boolean) = onResult(DiffValue.Leaf(value.toString()))
    override fun encodeByte(value: Byte) = onResult(DiffValue.Leaf(value.toString()))
    override fun encodeShort(value: Short) = onResult(DiffValue.Leaf(value.toString()))
    override fun encodeInt(value: Int) = onResult(DiffValue.Leaf(value.toString()))
    override fun encodeLong(value: Long) = onResult(DiffValue.Leaf(value.toString()))
    override fun encodeFloat(value: Float) = onResult(DiffValue.Leaf(value.toString()))
    override fun encodeDouble(value: Double) = onResult(DiffValue.Leaf(value.toString()))
    override fun encodeChar(value: Char) = onResult(DiffValue.Leaf("\"$value\""))
    override fun encodeString(value: String) = onResult(DiffValue.Leaf("\"$value\""))
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        onResult(DiffValue.Leaf(enumDescriptor.getElementName(index)))
    override fun encodeNull() = onResult(DiffValue.Leaf("null"))
    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        compositeFor(descriptor, serializersModule, onResult)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder =
        beginStructure(descriptor)

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer === BigIntegerSerializer) {
            onResult(DiffValue.Leaf(value.toString()))
        } else {
            serializer.serialize(this, value)
        }
    }
}

private fun compositeFor(
    descriptor: SerialDescriptor,
    serializersModule: SerializersModule,
    onResult: (DiffValue) -> Unit,
): CompositeEncoder = when {
    descriptor.kind is PolymorphicKind.SEALED -> SealedTreeEncoder(serializersModule, onResult)
    descriptor.kind == StructureKind.LIST -> ListTreeEncoder(descriptor, serializersModule, onResult)
    descriptor.kind == StructureKind.MAP -> MapTreeEncoder(serializersModule, onResult)
    else -> ClassTreeEncoder(serializersModule, onResult)
}

/**
 * Shared [CompositeEncoder] plumbing: every element write (primitive or composite) is delegated to
 * a fresh [ValueSlotEncoder] for that element, whose resulting [DiffValue] is handed to [place].
 */
private abstract class TreeCompositeEncoder(
    final override val serializersModule: SerializersModule,
    private val onResult: (DiffValue) -> Unit,
) : CompositeEncoder {

    protected abstract fun place(descriptor: SerialDescriptor, index: Int, value: DiffValue)
    protected abstract fun build(): DiffValue

    final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) =
        place(descriptor, index, DiffValue.Leaf(value.toString()))
    final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) =
        place(descriptor, index, DiffValue.Leaf(value.toString()))
    final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) =
        place(descriptor, index, DiffValue.Leaf(value.toString()))
    final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) =
        place(descriptor, index, DiffValue.Leaf(value.toString()))
    final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) =
        place(descriptor, index, DiffValue.Leaf(value.toString()))
    final override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) =
        place(descriptor, index, DiffValue.Leaf(value.toString()))
    final override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) =
        place(descriptor, index, DiffValue.Leaf(value.toString()))
    final override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) =
        place(descriptor, index, DiffValue.Leaf("\"$value\""))
    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) =
        place(descriptor, index, DiffValue.Leaf("\"$value\""))

    final override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder =
        ValueSlotEncoder(serializersModule) { place(descriptor, index, it) }

    final override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
    ) {
        val slot = ValueSlotEncoder(serializersModule) { place(descriptor, index, it) }
        slot.encodeSerializableValue(serializer, value)
    }

    final override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?,
    ) {
        if (value == null) {
            place(descriptor, index, DiffValue.Leaf("null"))
        } else {
            encodeSerializableElement(descriptor, index, serializer, value)
        }
    }

    final override fun endStructure(descriptor: SerialDescriptor) = onResult(build())
}

/** Fields whose element is annotated `@QuintIgnore` never make it into [fields], so they never take
 *  part in a [TypedState] comparison or diff. */
private class ClassTreeEncoder(
    serializersModule: SerializersModule,
    onResult: (DiffValue) -> Unit,
) : TreeCompositeEncoder(serializersModule, onResult) {
    private val fields = LinkedHashMap<String, DiffValue>()

    override fun place(descriptor: SerialDescriptor, index: Int, value: DiffValue) {
        if (descriptor.getElementAnnotations(index).any { it is QuintIgnore }) return
        fields[descriptor.getElementName(index)] = value
    }

    override fun build(): DiffValue = DiffValue.Struct(fields)
}

/** [ordered] is false only for `Set`/`LinkedHashSet`/`HashSet` targets, matching content-based diffing. */
private class ListTreeEncoder(
    descriptor: SerialDescriptor,
    serializersModule: SerializersModule,
    onResult: (DiffValue) -> Unit,
) : TreeCompositeEncoder(serializersModule, onResult) {
    private val ordered = descriptor.serialName != "kotlin.collections.LinkedHashSet" &&
        descriptor.serialName != "kotlin.collections.HashSet"
    private val items = mutableListOf<DiffValue>()

    override fun place(descriptor: SerialDescriptor, index: Int, value: DiffValue) {
        while (items.size <= index) items.add(DiffValue.Leaf("null"))
        items[index] = value
    }

    override fun build(): DiffValue = DiffValue.Seq(items, ordered)
}

/** Entries are exposed as `2 * size` slots (even = key, odd = value), matching kotlinx's map protocol. */
private class MapTreeEncoder(
    serializersModule: SerializersModule,
    onResult: (DiffValue) -> Unit,
) : TreeCompositeEncoder(serializersModule, onResult) {
    private val slots = mutableListOf<DiffValue>()

    override fun place(descriptor: SerialDescriptor, index: Int, value: DiffValue) {
        while (slots.size <= index) slots.add(DiffValue.Leaf("null"))
        slots[index] = value
    }

    override fun build(): DiffValue = DiffValue.MapNode(slots.chunked(2).map { (k, v) -> k to v })
}

/**
 * Flattens a `@JsonClassDiscriminator("tag") sealed class` variant into a single [DiffValue.Struct]
 * with a `tag` field plus the concrete subtype's own fields, matching how ITF represents a sum type
 * (see `ItfSealedDecoder`). Element 0 is always the discriminator string; element 1 is always the
 * concrete subtype re-encoded as its own structure.
 */
private class SealedTreeEncoder(
    serializersModule: SerializersModule,
    onResult: (DiffValue) -> Unit,
) : TreeCompositeEncoder(serializersModule, onResult) {
    private var tag: String? = null
    private var payload: DiffValue.Struct? = null

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        if (index == 0) tag = value else super.encodeStringElement(descriptor, index, value)
    }

    override fun place(descriptor: SerialDescriptor, index: Int, value: DiffValue) {
        if (index == 1 && value is DiffValue.Struct) payload = value
    }

    override fun build(): DiffValue {
        val fields = LinkedHashMap<String, DiffValue>()
        fields["tag"] = DiffValue.Leaf(tag ?: "?")
        payload?.fields?.let { fields.putAll(it) }
        return DiffValue.Struct(fields)
    }
}
