package io.github.mcbianconi.itf

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer

/**
 * Decodes this [ItfValue] into [T] using [deserializer].
 *
 * This normalizes the ITF encoding against [deserializer]'s [kotlinx.serialization.descriptors.SerialDescriptor]
 * first (see the mechanics documented on each [ItfValue] variant), which is what lets a
 * `Map<List<Long>, V>` / `Map<R, V>` field and an `Option[T]` field (`T?` in Kotlin) decode
 * correctly, including when empty/`None`.
 *
 * A record whose target type is nullable is treated as a Quint `Option[T]`: `{tag: "None"}`
 * decodes to `null`, `{tag: "Some", value: v}` decodes to `v`. This is the ITF/Quint convention
 * for `Option`, matching `itf-rs`'s `itf::de::Option` on the Rust side; it only applies to a
 * nullable target, so a user sum type with `Some`/`None` variants decodes as-is.
 */
public fun <T> ItfValue.decode(deserializer: DeserializationStrategy<T>): T =
    ItfJson.decodeFromJsonElement(deserializer, toNormalizedJson(deserializer.descriptor))

/** Reified overload of [decode] that resolves [T]'s serializer automatically. */
public inline fun <reified T> ItfValue.decode(): T = decode(serializer())
