---
type: is
id: is-01m3e7dfwyvcdbqdj15rrb4ta2
title: Define the :itf public API and keep JSON normalization internal
kind: task
status: closed
priority: 1
version: 5
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5ac50k1ks6sbctrx4q56t
  - type: blocks
    target: is-01m3e5aazh68bmk8rphx62grxq
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:46:04.958Z
updated_at: 2026-09-26T12:29:26.194Z
closed_at: 2026-09-26T12:29:26.189Z
close_reason: "Added ItfValue.decode as the single public decode entry point; made toNormalizedJson/QuintJson(renamed ItfJson) internal to :itf. core (State.check, NondetPicks.decode/decodeOrNull) switched to decode(), staying source-compatible. Updated README/CLAUDE/AGENTS and itf-* decision docs. Added ItfValueDecodeTest (6 tests: explicit + reified decode, Option Some/None, BigInteger fits/overflow). Committed as itf-public-api (rmq/15b79a8) stacked above itf-module. :itf:test 38/38, :core:test (NondetPicksTest+TypedStateTest) 13/13, :example:test 5/5 all pass."
resolution: null
duplicate_of: null
---
Public :itf surface: parseTrace, ItfValue, ItfTrace, ItfValueSerializer, display(), BigIntegerSerializer, intoOption(), and one decode entry point (e.g. fun <T> ItfValue.decode(serializer: KSerializer<T>): T plus a reified overload) that hides toNormalizedJson and the Json instance. core (State.check, NondetPicks.decode/decodeOrNull) switches to it; toNormalizedJson becomes internal and QuintJson becomes an internal ItfJson. The descriptor-gated Option unwrap (nullable target => {tag: None/Some} unwrapped) stays in :itf, documented as the ITF/Quint Option convention like itf-rs itf::de::Option; mbt:: handling stays in core. This lets qk-ze9b swap the JSON path for a custom Decoder without an API change. Update paths in docs/decisions/itf-option-and-bigint.md and itf-collection-mapping.md.
