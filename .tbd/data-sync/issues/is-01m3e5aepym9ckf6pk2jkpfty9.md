---
type: is
id: is-01m3e5aepym9ckf6pk2jkpfty9
title: Property-based tests for ITF decoding
kind: task
status: closed
priority: 3
version: 5
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T06:09:28.285Z
updated_at: 2026-09-26T14:43:08.354Z
closed_at: 2026-09-26T14:43:08.354Z
close_reason: "Added itf/src/test/.../ItfValuePropertyTest.kt: seeded property tests for ItfValueSerializer round-trip and ItfValue.decode against a hand-built oracle (3 seeds x 200 cases each). Found and filed qk-uhta (ItfValueSerializer doesn't decode ADR-015 #unserializable values), captured as a @Disabled regression test. Commit urx on branch itf-dx. :itf:test, :itf:checkKotlinAbi, :example:test all green."
resolution: null
duplicate_of: null
---
Round-trip property tests for ItfValue encode/decode and the kotlinx Decoder from qk-ze9b, using generated ItfValues.

## Notes

Targets the :itf module after qk-km9v.
