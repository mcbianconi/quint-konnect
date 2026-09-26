---
type: is
id: is-01m3dzg4j09s81zwsjhdyxfw8t
title: "Runner: catch Throwable so the reproduce-seed message is printed"
kind: bug
status: closed
priority: 1
version: 3
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
hold: null
hold_until: null
created_at: 2026-09-26T04:27:43.040Z
updated_at: 2026-09-26T04:36:37.902Z
started_at: 2026-09-26T04:33:32.749Z
closed_at: 2026-09-26T04:36:37.901Z
close_reason: Runner catches Throwable, wraps step failures with trace/step/action/nondet context, preserves original as cause
resolution: null
duplicate_of: null
---
core/.../Runner.kt:57 catches only Exception. JUnit/kotlin.test assertion failures are AssertionError (an Error), so the 'Reproduce with QUINT_SEED=...' line is skipped for the most common failure kind.
