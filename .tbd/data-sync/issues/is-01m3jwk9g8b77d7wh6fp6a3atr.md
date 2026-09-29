---
type: is
id: is-01m3jwk9g8b77d7wh6fp6a3atr
title: Enable class-level JUnit concurrency via junit-platform.properties
kind: task
status: closed
priority: 2
version: 2
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies: []
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:12.839Z
updated_at: 2026-09-29T01:28:23.518Z
closed_at: 2026-09-29T01:28:23.518Z
close_reason: "Phase 2 not needed: after Phase 1 the build is CPU-bound (every Test task slowed under org.gradle.parallel); ksp:test already forks; functionalTest is the critical path and JUnit class concurrency doesn't apply to it. See spec Results."
resolution: null
duplicate_of: null
---
Add src/test/resources/junit-platform.properties in each chosen module: parallel.enabled=true, mode.default=same_thread, mode.classes.default=concurrent, with a reference comment to the JUnit user guide. Re-measure, record in the spec, and repeat the 5-run + CI stability gate.
