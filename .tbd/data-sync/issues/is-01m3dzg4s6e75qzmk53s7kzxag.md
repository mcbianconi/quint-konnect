---
type: is
id: is-01m3dzg4s6e75qzmk53s7kzxag
title: "Runner: report trace index, step index and action on failure"
kind: bug
status: open
priority: 1
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfs74s438xapfcmfsmexa
created_at: 2026-09-26T04:27:43.269Z
updated_at: 2026-09-26T04:27:43.269Z
---
Runner.kt:33-53. A failure only shows the raw exception; wrap it with trace number, step number, action name and nondet picks so the diverging step is identifiable.
