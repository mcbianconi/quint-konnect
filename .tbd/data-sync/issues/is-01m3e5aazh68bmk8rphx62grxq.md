---
type: is
id: is-01m3e5aazh68bmk8rphx62grxq
title: Custom kotlinx Decoder over ItfValue
kind: feature
status: open
priority: 2
version: 3
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e5aepym9ckf6pk2jkpfty9
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:24.465Z
updated_at: 2026-09-26T06:49:24.936Z
---
Replace ItfValue -> JsonElement -> kotlinx with a kotlinx Decoder reading ItfValue directly. One decode path (no descriptor vs no-descriptor behavior split), errors named by Quint field path. Make toNormalizedJson internal.

## Notes

Lives in the :itf module after qk-km9v; implement behind the decode entry point from qk-pzl0.
