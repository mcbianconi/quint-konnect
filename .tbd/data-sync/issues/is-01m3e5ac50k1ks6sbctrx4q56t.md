---
type: is
id: is-01m3e5ac50k1ks6sbctrx4q56t
title: Enable explicit API mode and ABI validation
kind: chore
status: open
priority: 1
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:09:25.663Z
updated_at: 2026-09-26T06:09:25.663Z
---
No explicitApi() or ABI validation in any build script. NondetPicks.get returns ItfValue, leaking the ITF model. Decide the public surface and make the rest internal before publishing.
