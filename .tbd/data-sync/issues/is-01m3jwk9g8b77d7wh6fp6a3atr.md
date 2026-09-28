---
type: is
id: is-01m3jwk9g8b77d7wh6fp6a3atr
title: Enable class-level JUnit concurrency via junit-platform.properties
kind: task
status: open
priority: 2
version: 1
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies: []
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:12.839Z
updated_at: 2026-09-28T02:13:12.839Z
---
Add src/test/resources/junit-platform.properties in each chosen module: parallel.enabled=true, mode.default=same_thread, mode.classes.default=concurrent, with a reference comment to the JUnit user guide. Re-measure, record in the spec, and repeat the 5-run + CI stability gate.
