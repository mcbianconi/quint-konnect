---
name: itf-collection-mapping
date: 2026-09-26
---

Quint sets map to Kotlin `Set<T>`, not `List<T>`. Quint tuple-keyed maps map to
`Map<List<Long>, V>`, not `Map<Pair<A, B>, V>`.

**Why:** ITF gives no element order for sets, so `List<T>` made state comparison fail for
correct implementations. `Pair` doesn't work as a map key either: kotlinx.serialization's
`allowStructuredMapKeys` decodes a structured key from the flat `[k1, v1, ...]` array
form the (internal) normalizer produces, and `Pair` expects `{"first": ..., "second": ...}`,
not an array.

**How to apply:** Don't recommend `List<T>` for sets or `Pair` for tuple-keyed maps, even
though both look like the natural Kotlin fit. See `ItfValueNormalizer.kt`'s KDoc for the
current encoding mechanics; consumers decode through `ItfValue.decode`, not the internal
`toNormalizedJson`/`ItfJson`.
