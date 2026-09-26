---
type: is
id: is-01m3e5ac50k1ks6sbctrx4q56t
title: Enable explicit API mode and ABI validation
kind: chore
status: open
priority: 1
version: 3
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3dzh3h36exkjke6fkw32cph
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:09:25.663Z
updated_at: 2026-09-26T06:46:19.719Z
---
No explicitApi() or ABI validation in any build script. NondetPicks.get returns ItfValue, leaking the ITF model. Decide the public surface and make the rest internal before publishing.

## Notes

Apply per module (:itf, :core, :annotations, :ksp). After qk-km9v/qk-pzl0, NondetPicks.get returning ItfValue is fine: ItfValue is the published :itf API.
