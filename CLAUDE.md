## Project Decisions

See `docs/decisions/` for standing project decisions and constraints (e.g. platform
support). Check it for context, and add a new entry there for any similar decision
going forward.

## Representing Quint Types in Kotlin

| Quint type | Kotlin `@Serializable` type |
|------------|----------------------------|
| `int` | `Long` |
| `bool` | `Boolean` |
| `str` | `String` |
| `(int, int)` tuple | `List<Long>` (index 0 = `._1`, index 1 = `._2`) |
| `int -> V` map | `Map<Long, V>` |
| `Set[T]` | `List<T>` |
| `type P = X \| O` sum type | `@Serializable @JsonClassDiscriminator("tag") sealed class` |
| `type S = Foo(P) \| Bar` | `sealed class` with `data class Foo(val value: P)` and `object Bar` |

For sum types, add `@file:OptIn(ExperimentalSerializationApi::class)` at the top of the file.
