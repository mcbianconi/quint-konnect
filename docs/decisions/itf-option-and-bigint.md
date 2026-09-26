---
name: itf-option-and-bigint
date: 2026-09-26
---

Quint `Option[T]` maps to a nullable Kotlin type `T?`. `toNormalizedJson` unwraps
`{tag: "Some", value: v}` / `{tag: "None"}` only when the target `SerialDescriptor` is
nullable (bead qk-9sdm): `State.check` passes the state serializer's descriptor, and
`NondetPicks.decode`/`decodeOrNull` pass the reified type's descriptor. A non-nullable
target, including a user sealed class whose variants happen to be named `Some`/`None`,
is never unwrapped this way, so it decodes as an ordinary tagged record (bead qk-oqok).

`NondetPicks.fromRecord` keeps its own unconditional `intoOption()` unwrap: Quint's MBT
runner wraps *every* entry of `mbt::nondetPicks` in `Option` to mean "was this nondet
variable picked this step", one level, regardless of the pick's own type. That harness
wrapping is unambiguous (a user pick type using `Some`/`None` would appear one level
deeper, under the harness's `value` field) and is orthogonal to the descriptor-driven
unwrap inside a pick's own value.

Quint `int` values wider than `Long` normalize to an unquoted JSON number backed by
`BigInteger` (bead qk-mmbq); decode them with
`@Serializable(with = BigIntegerSerializer::class) val n: BigInteger`. There's no default
`KSerializer<BigInteger>` in kotlinx.serialization, so a field of this type needs the
explicit `with =`.

**Why:** The value alone can't tell "the target is `Option[T]`, unwrap" apart from "the
target is a user type that happens to look like one", so the decision needs the Kotlin
target's shape (via its descriptor), not the ITF value.

**How to apply:** Don't add a blanket `.intoOption()` call anywhere in the normalizer;
gate it on `descriptor.isNullable`. Don't reach for `BigDecimal` for oversized Quint
`int`s; Quint integers are never fractional.
