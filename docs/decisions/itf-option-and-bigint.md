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
orthogonal to the descriptor-driven `Option[T]` unwrap `toNormalizedJson` does for a
pick's own value (see its KDoc).

**How to apply:** Don't add a blanket `.intoOption()` call anywhere else in the
normalizer; the harness-level unwrap in `fromRecord` is the only unconditional one, and
every other layer must gate on the target descriptor being nullable. Don't reach for
`BigDecimal` for oversized Quint `int`s (`BigIntegerSerializer`) — Quint integers are
never fractional.
