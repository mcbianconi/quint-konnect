---
type: is
id: is-01m3jwk7m0ej0n6ke2ek94qp49
title: Baseline test timings for ./gradlew build
kind: task
status: closed
priority: 2
version: 6
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
delegate: claude-code@macmurillo.local
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk7vckrbbewwcbnxjt8fg
  - type: blocks
    target: is-01m3jwk82wymkw5g6v4dwmcf0x
  - type: blocks
    target: is-01m3jwk8adgge7yppcd47gnj2n
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
hold: null
hold_until: null
created_at: 2026-09-28T02:13:10.911Z
updated_at: 2026-09-28T02:24:10.192Z
started_at: 2026-09-28T02:20:44.462Z
closed_at: 2026-09-28T02:24:10.192Z
close_reason: "Baseline recorded in spec Results: local build --rerun-tasks total 1m23s (ksp:test 24.65s, functionalTest 24.73s tied for critical path); CI avg ~2m48s over 3 green main runs; no ksp memory pressure evidence."
resolution: null
duplicate_of: null
---
Run ./gradlew build --rerun-tasks on current main, locally and once on CI. Record total time and each Test task duration (Gradle MCP query_build), plus peak memory of ksp test forks, in the spec Results section. These numbers set MAX_FORKS/divisor and decide whether Phase 2 happens.
