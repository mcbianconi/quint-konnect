# Quint to Kotlin type mapping

Every state field and nondet pick decodes from an ITF value against a `@Serializable`
Kotlin type through `ItfValue.decode`. Use this table for spec state classes
(`TypedState`'s type parameter) and `@QuintAction` parameter types alike.

| Quint type | Kotlin `@Serializable` type |
|---|---|
| `int` | `Long` |<!--id:int-->
| `int` (larger than `Long`) | `@Contextual BigInteger` |<!--id:int-bignum-->
| `bool` | `Boolean` |<!--id:bool-->
| `str` | `String` |<!--id:str-->
| `(int, int)` tuple | `List<Long>` (index 0 = `._1`, index 1 = `._2`) |<!--id:tuple-->
| `int -> V` map | `Map<Long, V>` |<!--id:int-map-->
| `(int, int) -> V` map | `Map<List<Long>, V>` (not `Map<Pair<Long, Long>, V>`) |<!--id:tuple-map-->
| record-keyed map | `Map<R, V>`, `R` a `@Serializable` data class |<!--id:record-map-->
| `Set[T]` | `Set<T>` |<!--id:set-->
| `Option[T]` | `T?` |<!--id:option-->
| `type P = X \| O` sum type | `@Serializable @JsonClassDiscriminator("tag") sealed class` |<!--id:sum-simple-->
| `type S = Foo(P) \| Bar` | sealed class with `data class Foo(val value: P)` and `object Bar` |<!--id:sum-payload-->

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

`Map<List<Long>, V>` and `Map<R, V>` fields (the tuple-keyed and record-keyed map rows above)
decode correctly when empty too, the same as `Option[T]`'s `None` case — `ItfValue.decode` reads
the ITF value tree directly rather than going through an intermediate JSON representation, so an
empty `{"#map": []}` doesn't need special-casing per key type.

## Nondet picks use the same table

A `@QuintAction` method parameter decodes the same way: its Kotlin type follows this table, and
a nullable parameter type (`T?`) marks the pick as optional for that action (see the main
SKILL.md's driver-mapping section).

## Examples

Machine-checked by `itf/src/test/.../TypesMdDocTest.kt`. Each example decodes as the single
field `v` of a one-field wrapper record, using the row's declared Kotlin type for `v`. An
example id must start with its row's id (optionally followed by `-` and a variant name, e.g.
`option-some`); every row id above must be covered by at least one example.

<!-- example:int -->
```json
{"v": 42}
```

<!-- example:int-bignum -->
```json
{"v": {"#bigint": "123456789012345678901234567890"}}
```

<!-- example:bool -->
```json
{"v": true}
```

<!-- example:str -->
```json
{"v": "hello"}
```

<!-- example:tuple -->
```json
{"v": {"#tup": [1, 2]}}
```

<!-- example:int-map -->
```json
{"v": {"#map": [[1, "a"], [2, "b"]]}}
```

<!-- example:tuple-map -->
```json
{"v": {"#map": [[{"#tup": [1, 2]}, "a"]]}}
```

<!-- example:tuple-map-empty -->
```json
{"v": {"#map": []}}
```

<!-- example:record-map -->
```json
{"v": {"#map": [[{"x": 1, "y": 2}, "a"]]}}
```

<!-- example:record-map-empty -->
```json
{"v": {"#map": []}}
```

<!-- example:set -->
```json
{"v": {"#set": [1, 2, 3]}}
```

<!-- example:option-some -->
```json
{"v": {"tag": "Some", "value": 42}}
```

<!-- example:option-none -->
```json
{"v": {"tag": "None"}}
```

<!-- example:sum-simple -->
```json
{"v": {"tag": "X", "value": {"#tup": []}}}
```

<!-- example:sum-payload -->
```json
{"v": {"tag": "Foo", "value": 5}}
```
