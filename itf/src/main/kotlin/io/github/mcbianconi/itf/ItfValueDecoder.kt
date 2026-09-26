@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.itf

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import kotlin.collections.List as KList

/**
 * Thrown by [ItfValue.decode] when an [ItfValue] doesn't have the shape the target type expects.
 *
 * [message] names the Quint field path (e.g. `board.(1, 2)`, `move.tag`) relative to the value
 * passed to [ItfValue.decode], and the expected vs. actual ITF kind. Not public API: callers catch
 * the supertype [SerializationException] (see `checkKotlinAbi` in AGENTS.md — a new public class
 * here would be an API change).
 */
internal class ItfDecodingException(message: String, cause: Throwable? = null) :
    SerializationException(message, cause)

private fun fail(path: String, expected: String, actual: ItfValue): Nothing =
    throw ItfDecodingException(
        "${path.ifEmpty { "<root>" }}: expected $expected, got ${actual.kindDescription()} (${actual.display()})",
    )

private fun ItfValue.kindDescription(): String = when (this) {
    is ItfValue.Bool -> "a boolean"
    is ItfValue.Num, is ItfValue.BigInt -> "an int"
    is ItfValue.Str -> "a string"
    is ItfValue.List -> "a list"
    is ItfValue.Tup -> "a tuple"
    is ItfValue.Set -> "a set"
    is ItfValue.Map -> "a map"
    is ItfValue.Record -> "a record"
    is ItfValue.Unserializable -> "an unserializable value"
}

private fun String.field(name: String): String = if (isEmpty()) name else "$this.$name"

private fun String.index(i: Int): String = "$this[$i]"

private fun ItfValue.asOrderedChildren(): KList<ItfValue>? = when (this) {
    is ItfValue.List -> values
    is ItfValue.Tup -> values
    is ItfValue.Set -> values
    else -> null
}

/**
 * A [Decoder] that reads a single [ItfValue] node directly, without an intermediate JSON tree.
 *
 * [path] names this node's Quint field path for error messages (e.g. `board.(1, 2)`). The root
 * [ItfValue.decode] call starts with an empty path, since it has no caller-supplied root name;
 * nested paths are built as children are decoded (record field names, `[index]` for list/tuple/set
 * elements, and the map key's own [display] for map entries).
 */
internal class ItfValueDecoder(
    initialValue: ItfValue,
    private val path: String,
    override val serializersModule: SerializersModule = itfSerializersModule,
) : Decoder {

    private var value: ItfValue = initialValue

    /** Exposes the current raw value so [BigIntegerSerializer] can read it without going through JSON. */
    internal val rawValue: ItfValue get() = value

    internal val rawPath: String get() = path

    /**
     * Unwraps a Quint `Option` value: `{tag: "None"}` -> not-null-mark `false`, `{tag: "Some", value:
     * v}` -> not-null-mark `true` and the current value becomes `v`. kotlinx.serialization only calls
     * this when the target position is genuinely nullable, so it never touches a non-nullable field
     * even when that field's raw value happens to look like an Option (see
     * `docs/decisions/itf-option-and-bigint.md`).
     */
    override fun decodeNotNullMark(): Boolean {
        val record = value as? ItfValue.Record ?: return true
        return when (val unwrapped = record.intoOption()) {
            null -> false
            record -> true
            else -> {
                value = unwrapped
                true
            }
        }
    }

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = (value as? ItfValue.Bool)?.value ?: fail(path, "a boolean", value)

    override fun decodeLong(): Long = when (val v = value) {
        is ItfValue.Num -> v.value
        is ItfValue.BigInt -> v.value.toLongOrNull() ?: fail(path, "an int that fits in a 64-bit Long", v)
        else -> fail(path, "an int", v)
    }

    override fun decodeInt(): Int {
        val n = decodeLong()
        if (n !in Int.MIN_VALUE..Int.MAX_VALUE) fail(path, "an int that fits in a 32-bit Int", value)
        return n.toInt()
    }

    override fun decodeShort(): Short {
        val n = decodeLong()
        if (n !in Short.MIN_VALUE..Short.MAX_VALUE) fail(path, "an int that fits in a 16-bit Short", value)
        return n.toShort()
    }

    override fun decodeByte(): Byte {
        val n = decodeLong()
        if (n !in Byte.MIN_VALUE..Byte.MAX_VALUE) fail(path, "an int that fits in an 8-bit Byte", value)
        return n.toByte()
    }

    override fun decodeDouble(): Double = when (val v = value) {
        is ItfValue.Num -> v.value.toDouble()
        is ItfValue.BigInt -> v.value.toDoubleOrNull() ?: fail(path, "a number", v)
        else -> fail(path, "a number", v)
    }

    override fun decodeFloat(): Float = decodeDouble().toFloat()

    override fun decodeChar(): Char = decodeString().singleOrNull() ?: fail(path, "a single-character string", value)

    override fun decodeString(): String = when (val v = value) {
        is ItfValue.Str -> v.value
        is ItfValue.Unserializable -> v.value
        else -> fail(path, "a string", v)
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val s = decodeString()
        val index = enumDescriptor.getElementIndex(s)
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            fail(path, "one of ${enumDescriptor.elementNames.toList()}", ItfValue.Str(s))
        }
        return index
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = when {
        descriptor.kind is PolymorphicKind.SEALED -> {
            val record = value as? ItfValue.Record ?: fail(path, "a sum type variant record", value)
            ItfSealedDecoder(record, path, serializersModule)
        }
        descriptor.kind == StructureKind.LIST -> {
            val children = value.asOrderedChildren() ?: fail(path, "a list, tuple or set", value)
            ItfListDecoder(children, path, serializersModule)
        }
        descriptor.kind == StructureKind.MAP -> {
            val entries = (value as? ItfValue.Map)?.entries ?: fail(path, "a map", value)
            ItfMapDecoder(entries, path, serializersModule)
        }
        descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> {
            val fields = (value as? ItfValue.Record)?.fields ?: fail(path, "a record", value)
            ItfClassDecoder(fields, path, serializersModule)
        }
        else -> throw ItfDecodingException("${path.ifEmpty { "<root>" }}: unsupported descriptor kind ${descriptor.kind}")
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = try {
        deserializer.deserialize(this)
    } catch (e: ItfDecodingException) {
        throw e
    } catch (e: SerializationException) {
        throw ItfDecodingException("${path.ifEmpty { "<root>" }}: ${e.message}", e)
    }
}

/**
 * Shared [CompositeDecoder] plumbing: every element decode (primitive or composite) is delegated to
 * a fresh [ItfValueDecoder] for that element, built by [elementDecoder]. Never validates for unknown
 * ITF fields/entries, matching the pre-decoder `ignoreUnknownKeys` behaviour.
 */
private abstract class ItfCompositeDecoder(
    override val serializersModule: SerializersModule,
) : CompositeDecoder {

    protected abstract fun elementDecoder(descriptor: SerialDescriptor, index: Int): ItfValueDecoder

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
        elementDecoder(descriptor, index).decodeString()

    final override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
        elementDecoder(descriptor, index).decodeBoolean()

    final override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
        elementDecoder(descriptor, index).decodeByte()

    final override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
        elementDecoder(descriptor, index).decodeShort()

    final override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
        elementDecoder(descriptor, index).decodeInt()

    final override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
        elementDecoder(descriptor, index).decodeLong()

    final override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
        elementDecoder(descriptor, index).decodeFloat()

    final override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
        elementDecoder(descriptor, index).decodeDouble()

    final override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
        elementDecoder(descriptor, index).decodeChar()

    final override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder =
        elementDecoder(descriptor, index)

    final override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T = elementDecoder(descriptor, index).decodeSerializableValue(deserializer)

    final override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
    ): T? = elementDecoder(descriptor, index).decodeNullableSerializableValue(deserializer)

    override fun endStructure(descriptor: SerialDescriptor) {}
}

/** Decodes an [ItfValue.Record] into a `@Serializable` class/object, tolerating unknown ITF fields. */
private class ItfClassDecoder(
    private val fields: LinkedHashMap<String, ItfValue>,
    private val path: String,
    serializersModule: SerializersModule,
) : ItfCompositeDecoder(serializersModule) {

    private var position = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < descriptor.elementsCount) {
            val index = position++
            if (descriptor.getElementName(index) in fields) return index
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun elementDecoder(descriptor: SerialDescriptor, index: Int): ItfValueDecoder {
        val name = descriptor.getElementName(index)
        return ItfValueDecoder(fields.getValue(name), path.field(name), serializersModule)
    }
}

/** Decodes an [ItfValue.List], [ItfValue.Tup] or [ItfValue.Set] into a `List`/`Set` target. */
private class ItfListDecoder(
    private val children: KList<ItfValue>,
    private val path: String,
    serializersModule: SerializersModule,
) : ItfCompositeDecoder(serializersModule) {

    private var position = 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = children.size

    override fun decodeSequentially(): Boolean = true

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        if (position < children.size) position++ else CompositeDecoder.DECODE_DONE

    override fun elementDecoder(descriptor: SerialDescriptor, index: Int): ItfValueDecoder =
        ItfValueDecoder(children[index], path.index(index), serializersModule)
}

/**
 * Decodes an [ItfValue.Map] into a `Map<K, V>` target, for any key shape (primitive, enum, tuple,
 * record or sum type): entries are exposed as `2 * entries.size` slots (even = key, odd = value),
 * matching kotlinx's generic map decoding protocol, so no primitive-vs-structured-key split is
 * needed (unlike the old JSON-based normalizer, this also makes an empty map decode correctly
 * regardless of its key type).
 */
private class ItfMapDecoder(
    private val entries: KList<Pair<ItfValue, ItfValue>>,
    private val path: String,
    serializersModule: SerializersModule,
) : ItfCompositeDecoder(serializersModule) {

    private var position = 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = entries.size

    override fun decodeSequentially(): Boolean = true

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        if (position < entries.size * 2) position++ else CompositeDecoder.DECODE_DONE

    override fun elementDecoder(descriptor: SerialDescriptor, index: Int): ItfValueDecoder {
        val (key, entryValue) = entries[index / 2]
        val entryPath = path.field(key.display())
        return if (index % 2 == 0) {
            ItfValueDecoder(key, entryPath, serializersModule)
        } else {
            ItfValueDecoder(entryValue, entryPath, serializersModule)
        }
    }
}

/**
 * Decodes an [ItfValue.Record] shaped `{tag: "VariantName", value: ...}` as a
 * `@JsonClassDiscriminator("tag") sealed class` variant. Always reads the discriminator from the
 * `"tag"` field regardless of the `@JsonClassDiscriminator` annotation's own name, since that's the
 * fixed shape Quint emits.
 *
 * Follows the same two-step protocol JSON uses for a flat (non-nested) discriminator:
 * [decodeStringElement] returns the discriminator (element 0), and [elementDecoder] for element 1
 * re-decodes the *same* record (not just its `value` field) as the concrete subclass, whose own
 * `value` property then picks up the payload. The `"tag"` key is simply never looked up by the
 * subclass's own [ItfClassDecoder], so it's ignored like any other unknown field.
 */
private class ItfSealedDecoder(
    private val record: ItfValue.Record,
    private val path: String,
    serializersModule: SerializersModule,
) : ItfCompositeDecoder(serializersModule) {

    private var position = 0

    override fun decodeSequentially(): Boolean = true

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        if (position < 2) position++ else CompositeDecoder.DECODE_DONE

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        val tagPath = path.field("tag")
        val tagValue = record.fields["tag"]
        val tag = tagValue as? ItfValue.Str
            ?: fail(tagPath, "a string \"tag\" field naming the sum type variant", tagValue ?: record)
        val validNames = descriptor.getElementDescriptor(1).elementNames.toList()
        if (tag.value !in validNames) fail(tagPath, "one of $validNames", tag)
        return tag.value
    }

    override fun elementDecoder(descriptor: SerialDescriptor, index: Int): ItfValueDecoder =
        ItfValueDecoder(record, path, serializersModule)
}
