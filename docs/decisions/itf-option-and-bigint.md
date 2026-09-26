---
name: itf-option-and-bigint
date: 2026-09-26
---

`NondetPicks.fromRecord` unconditionally unwraps one `Option` layer off every entry of
`mbt::nondetPicks`, regardless of the pick's own type.

**Why:** Quint's MBT runner wraps *every* entry of `mbt::nondetPicks` in `Option` to mean
"was this nondet variable picked this step", one level, independent of the pick's own
type. That harness-level wrapping is unambiguous — a user pick type that itself uses
`Some`/`None` would appear one level deeper, under the harness's `value` field — and is
orthogonal to the descriptor-driven `Option[T]` unwrap `ItfValue.decode` does for a
pick's own value (see its KDoc).

**How to apply:** Don't add a blanket `.intoOption()` call anywhere else in the
decoder; the harness-level unwrap in `fromRecord` is the only unconditional one. Every
other layer must gate on the target position being nullable — `ItfValueDecoder.decodeNotNullMark()`
does the `intoOption()`-based unwrap, and kotlinx.serialization only ever calls
`decodeNotNullMark()` for a genuinely nullable target, so the gating falls out of the
decoder protocol rather than an explicit `descriptor.isNullable` check. Don't reach for
`BigDecimal` for oversized Quint `int`s (`BigIntegerSerializer`) — Quint integers are
never fractional.
