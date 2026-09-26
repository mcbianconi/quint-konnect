# Quint to Kotlin type mapping

Every state field and nondet pick decodes from an ITF value against a `@Serializable`
Kotlin type through `ItfValue.decode`. Use this table for spec state classes
(`TypedState`'s type parameter) and `@QuintAction` parameter types alike.

| Quint type | Kotlin `@Serializable` type |
|---|---|
| `int` | `Long` |
| `int` (larger than `Long`) | `@Contextual BigInteger` |
| `bool` | `Boolean` |
| `str` | `String` |
| `(int, int)` tuple | `List<Long>` (index 0 = `._1`, index 1 = `._2`) |
| `int -> V` map | `Map<Long, V>` |
| `(int, int) -> V` map | `Map<List<Long>, V>` (not `Map<Pair<Long, Long>, V>`) |
| record-keyed map | `Map<R, V>`, `R` a `@Serializable` data class |
| `Set[T]` | `Set<T>` |
| `Option[T]` | `T?` |
| `type P = X \| O` sum type | `@Serializable @JsonClassDiscriminator("tag") sealed class` |
| `type S = Foo(P) \| Bar` | sealed class with `data class Foo(val value: P)` and `object Bar` |

For sum types, add `@file:OptIn(ExperimentalSerializationApi::class)` at the top of the file, and
give each variant a `@SerialName` matching its Quint constructor name.

## Why `Set<T>` and `Map<List<Long>, V>`, not `List<T>`/`Pair`

ITF gives no element order for sets, so `List<T>` makes state comparison fail for correct
implementations that happen to build the set in a different order. `Pair` doesn't work as a
tuple-keyed map's key either: the map decoder hands each key to the target key type's own
deserializer, and a tuple's ITF shape (`{"#tup": [1, 2]}`, decoded positionally) doesn't match
what `Pair`'s generated deserializer expects (a `{"first": ..., "second": ...}` object). See
`docs/decisions/itf-collection-mapping.md` in the quint-konnect repo.

## `Option[T]` and oversized integers

`Option[T]` decodes as Kotlin's own nullable `T?` — no wrapper type needed, including for `None`
inside a map value or nested field. Reach for `@Contextual BigInteger` (never `BigDecimal`) only
for a Quint `int` that can exceed `Long`'s range; Quint integers are never fractional. See
`docs/decisions/itf-option-and-bigint.md`.

## Nondet picks use the same table

A `@QuintAction` method parameter decodes the same way: its Kotlin type follows this table, and
a nullable parameter type (`T?`) marks the pick as optional for that action (see the main
SKILL.md's driver-mapping section).
