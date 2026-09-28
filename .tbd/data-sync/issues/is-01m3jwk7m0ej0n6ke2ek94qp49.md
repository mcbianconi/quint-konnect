---
type: is
id: is-01m3jwk7m0ej0n6ke2ek94qp49
title: Baseline test timings for ./gradlew build
kind: task
status: open
priority: 2
version: 4
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk7vckrbbewwcbnxjt8fg
  - type: blocks
    target: is-01m3jwk82wymkw5g6v4dwmcf0x
  - type: blocks
    target: is-01m3jwk8adgge7yppcd47gnj2n
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
created_at: 2026-09-28T02:13:10.911Z
updated_at: 2026-09-28T02:13:11.628Z
---
Run ./gradlew build --rerun-tasks on current main, locally and once on CI. Record total time and each Test task duration (Gradle MCP query_build), plus peak memory of ksp test forks, in the spec Results section. These numbers set MAX_FORKS/divisor and decide whether Phase 2 happens.
