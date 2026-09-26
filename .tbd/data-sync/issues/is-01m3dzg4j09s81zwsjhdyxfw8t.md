---
type: is
id: is-01m3dzg4j09s81zwsjhdyxfw8t
title: "Runner: catch Throwable so the reproduce-seed message is printed"
kind: bug
status: open
priority: 1
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:43.040Z
updated_at: 2026-09-26T04:27:43.040Z
---
core/.../Runner.kt:57 catches only Exception. JUnit/kotlin.test assertion failures are AssertionError (an Error), so the 'Reproduce with QUINT_SEED=...' line is skipped for the most common failure kind.
