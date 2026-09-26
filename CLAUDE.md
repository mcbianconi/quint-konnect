## Project Decisions

See `docs/decisions/` for standing project decisions and constraints (e.g. platform
support). Check it for context. Add a new entry there only when the decision isn't
already derivable from the code, tests, KDoc, or other docs — if it is, put it there
instead (a code comment, an error message) rather than in a separate file.

## Representing Quint Types in Kotlin

| Quint type | Kotlin `@Serializable` type |
|------------|----------------------------|
| `int` | `Long` |
| `int` (larger than `Long`) | `BigInteger`, with `@Serializable(with = BigIntegerSerializer::class)` |
| `bool` | `Boolean` |
| `str` | `String` |
| `(int, int)` tuple | `List<Long>` (index 0 = `._1`, index 1 = `._2`) |
| `int -> V` map | `Map<Long, V>` |
| `(int, int) -> V` map | `Map<List<Long>, V>` (not `Map<Pair<Long, Long>, V>`) |
| record-keyed map | `Map<R, V>`, `R` a `@Serializable` data class |
| `Set[T]` | `Set<T>` |
| `Option[T]` | `T?` |
| `type P = X \| O` sum type | `@Serializable @JsonClassDiscriminator("tag") sealed class` |
| `type S = Foo(P) \| Bar` | `sealed class` with `data class Foo(val value: P)` and `object Bar` |

For sum types, add `@file:OptIn(ExperimentalSerializationApi::class)` at the top of the file.

`Map<List<Long>, V>` / `Map<R, V>` and `Option[T]` fields only decode correctly (including when
empty/`None`) through `State`/`NondetPicks.decode`, which pass the field's `SerialDescriptor` into
the normalizer. See `docs/decisions/itf-collection-mapping.md` and
`docs/decisions/itf-option-and-bigint.md`.
