---
type: is
id: is-01m3e5ackze94enpnzzrwnntm2
title: Shrink failing traces by maxSteps
kind: feature
status: in_progress
priority: 3
version: 3
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
hold: null
hold_until: null
created_at: 2026-09-26T06:09:26.143Z
updated_at: 2026-09-27T19:42:35.626Z
started_at: 2026-09-27T19:42:35.624Z
---
On failure, find a shorter failing trace by rerunning quint with the same seed and smaller maxSteps. Runs as a Gradle task (not at test time), so tests stay quint-free per qk-adm2: the failing replay records seed and trace, and the shrink task regenerates and replays until the shortest failing maxSteps is found.
