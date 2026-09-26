package io.github.mcbianconi.itf

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer
import java.math.BigInteger

/**
 * The [SerializersModule] every [ItfValueDecoder] decodes with, registering [BigIntegerSerializer]
 * contextually so a plain `val n: @Contextual BigInteger` field decodes without also needing
 * `@Serializable(with = BigIntegerSerializer::class)`.
 */
internal val itfSerializersModule: SerializersModule = SerializersModule {
    contextual(BigInteger::class, BigIntegerSerializer)
}

/**
 * Decodes this [ItfValue] into [T] using [deserializer].
 *
 * This reads the [ItfValue] tree directly through a custom [kotlinx.serialization.encoding.Decoder]
 * (see [ItfValueDecoder]), which is what lets a `Map<List<Long>, V>` / `Map<R, V>` field and an
 * `Option[T]` field (`T?` in Kotlin) decode correctly, including when empty/`None`.
 *
 * A record whose target type is nullable is treated as a Quint `Option[T]`: `{tag: "None"}`
 * decodes to `null`, `{tag: "Some", value: v}` decodes to `v`. This is the ITF/Quint convention
 * for `Option`, matching `itf-rs`'s `itf::de::Option` on the Rust side; it only applies to a
 * nullable target, so a user sum type with `Some`/`None` variants decodes as-is.
 *
 * A `BigInteger` field only needs `@Contextual` (not `@Serializable(with =
 * BigIntegerSerializer::class)`): this decoder's [SerializersModule] registers
 * [BigIntegerSerializer] contextually.
 *
 * Throws a [kotlinx.serialization.SerializationException] naming the Quint field path (e.g.
 * `board.(1, 2)`) and the expected vs. actual ITF kind when this value doesn't have the shape
 * [deserializer] expects.
 */
public fun <T> ItfValue.decode(deserializer: DeserializationStrategy<T>): T =
    ItfValueDecoder(this, path = "").decodeSerializableValue(deserializer)

/** Reified overload of [decode] that resolves [T]'s serializer automatically. */
public inline fun <reified T> ItfValue.decode(): T = decode(serializer())
