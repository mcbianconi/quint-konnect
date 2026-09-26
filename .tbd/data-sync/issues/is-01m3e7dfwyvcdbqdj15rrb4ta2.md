---
type: is
id: is-01m3e7dfwyvcdbqdj15rrb4ta2
title: Define the :itf public API and keep JSON normalization internal
kind: task
status: open
priority: 1
version: 3
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5ac50k1ks6sbctrx4q56t
  - type: blocks
    target: is-01m3e5aazh68bmk8rphx62grxq
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:46:04.958Z
updated_at: 2026-09-26T06:46:10.625Z
---
Public :itf surface: parseTrace, ItfValue, ItfTrace, ItfValueSerializer, display(), BigIntegerSerializer, intoOption(), and one decode entry point (e.g. fun <T> ItfValue.decode(serializer: KSerializer<T>): T plus a reified overload) that hides toNormalizedJson and the Json instance. core (State.check, NondetPicks.decode/decodeOrNull) switches to it; toNormalizedJson becomes internal and QuintJson becomes an internal ItfJson. The descriptor-gated Option unwrap (nullable target => {tag: None/Some} unwrapped) stays in :itf, documented as the ITF/Quint Option convention like itf-rs itf::de::Option; mbt:: handling stays in core. This lets qk-ze9b swap the JSON path for a custom Decoder without an API change. Update paths in docs/decisions/itf-option-and-bigint.md and itf-collection-mapping.md.
