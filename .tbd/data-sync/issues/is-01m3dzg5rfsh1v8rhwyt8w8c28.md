---
type: is
id: is-01m3dzg5rfsh1v8rhwyt8w8c28
title: "TraceGenerator: drain quint stdout/stderr concurrently and add a timeout"
kind: bug
status: closed
priority: 3
version: 3
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:44.270Z
updated_at: 2026-09-26T05:34:34.569Z
closed_at: 2026-09-26T05:34:34.564Z
close_reason: Implemented concurrent stdout/stderr draining and timeout support with tests passing
resolution: null
duplicate_of: null
---
trace/TraceGenerator.kt:14-23 never reads stdout, reads stderr only after waitFor(), no timeout. Low probability at verbosity 0 but a hang is hard to diagnose.
