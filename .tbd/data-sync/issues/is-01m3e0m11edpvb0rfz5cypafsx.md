---
type: is
id: is-01m3e0m11edpvb0rfz5cypafsx
title: Update stale ItfValue KDoc for Set and Map
kind: chore
status: closed
priority: 4
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:47:19.085Z
updated_at: 2026-09-26T06:21:33.833Z
closed_at: 2026-09-26T06:21:33.832Z
close_reason: "Fixed stale KDoc in ItfValue.kt for Set (was: maps to List; now: Set<T>), Map (was: always JSON object with string keys; now: JSON object for primitive/enum keys, flat array for tuple/record/sum keys), and BigInt (added BigInteger fallback + BigIntegerSerializer note). :core:compileKotlin verified via Gradle MCP (BUILD SUCCESSFUL). Committed as lmo on docs-license-ci."
resolution: null
duplicate_of: null
---
itf/ItfValue.kt KDoc still says Set maps to Kotlin List and Map becomes a JSON object with string keys; both changed in itf-sets-and-map-keys.
