---
name: itf-collection-mapping
date: 2026-09-26
---

Quint sets map to Kotlin `Set<T>`, not `List<T>`. Quint tuple-keyed maps map to
`Map<List<Long>, V>`, not `Map<Pair<A, B>, V>`.

**Why:** ITF gives no element order for sets, so `List<T>` made state comparison fail for
correct implementations. `Pair` doesn't work as a map key either: `ItfValueDecoder`'s map
decoding hands each key to the target key type's own deserializer directly (a `StructureKind.LIST`
descriptor's decoder reads the key's `Tup` entries positionally), and `Pair`'s deserializer expects
a `StructureKind.CLASS` shape (`{"first": ..., "second": ...}`), not a list.

**How to apply:** Don't recommend `List<T>` for sets or `Pair` for tuple-keyed maps, even
though both look like the natural Kotlin fit. See `ItfValueDecoder.kt`'s KDoc for the
current decoding mechanics; consumers decode through `ItfValue.decode`, which reads the
`ItfValue` tree directly (no intermediate JSON step).
