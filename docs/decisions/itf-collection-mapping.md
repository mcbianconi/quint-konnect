---
name: itf-collection-mapping
date: 2026-09-26
---

Quint sets map to Kotlin `Set<T>`. Quint maps with tuple, record or sum-type keys
normalize to a flat `[k1, v1, k2, v2, ...]` JSON array and decode through
`QuintJson`'s `allowStructuredMapKeys`; users write `Map<List<Long>, V>` for tuple keys
and `Map<R, V>` for record or sum-type keys. Maps with only `int`/`str`/`bool` keys stay
JSON objects with string keys.

**Why:** ITF gives no element order for sets, so `List<T>` made state comparison fail
for correct implementations. kotlinx.serialization decodes complex map keys only in the
structured-key array form. `Map<Pair<A, B>, V>` does not work: `Pair` expects
`{"first", "second"}`, not the array a tuple normalizes to.

**How to apply:** Keep the type tables in README.md and CLAUDE.md in line with this.
Don't recommend `List<T>` for sets or `Pair` for tuples. `toNormalizedJson` takes an
optional target `SerialDescriptor` (bead qk-kl73): `State.check` and
`NondetPicks.decode`/`decodeOrNull` pass the field's serializer descriptor, so an empty
tuple/record-keyed map picks the flat-array shape from the key descriptor's kind instead
of guessing from (zero) entries. Calling `toNormalizedJson()` with no descriptor keeps
the old value-only heuristic, so an empty map still normalizes to `{}` in that path.
