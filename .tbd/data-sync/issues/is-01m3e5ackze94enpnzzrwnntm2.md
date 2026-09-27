---
type: is
id: is-01m3e5ackze94enpnzzrwnntm2
title: Shrink failing traces by maxSteps
kind: feature
status: closed
priority: 3
version: 4
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
hold: null
hold_until: null
created_at: 2026-09-26T06:09:26.143Z
updated_at: 2026-09-27T19:55:20.422Z
started_at: 2026-09-27T19:42:35.624Z
closed_at: 2026-09-27T19:55:20.420Z
close_reason: "Landed 75cec2c: shrinkQuintTraces, a dedicated Test task over test's classes, runs quint itself with quintkonnect.shrink set; ReplayRunner reruns the first failing quint-run trace with the same seed at --max-steps 0..k-1 and reports/saves the shortest failure. Design deviation from the bead text: quint runs inside that Test task's JVM rather than a non-test Gradle task, which keeps the regular test task quint-free while reusing the JUnit harness and driver classpath."
resolution: null
duplicate_of: null
---
On failure, find a shorter failing trace by rerunning quint with the same seed and smaller maxSteps. Runs as a Gradle task (not at test time), so tests stay quint-free per qk-adm2: the failing replay records seed and trace, and the shrink task regenerates and replays until the shortest failing maxSteps is found.
