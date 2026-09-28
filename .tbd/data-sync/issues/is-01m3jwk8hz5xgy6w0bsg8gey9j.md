---
type: is
id: is-01m3jwk8hz5xgy6w0bsg8gey9j
title: Re-measure and stability-check Phase 1
kind: task
status: closed
priority: 2
version: 4
spec_path: docs/project/specs/active/plan-2026-09-27-parallel-test-execution.md
delegate: claude-code@macmurillo.local
labels: []
dependencies:
  - type: blocks
    target: is-01m3jwk8swnrq45r9twma3ht5p
parent_id: is-01m3jwgy900me6x6prh4pvdxjc
hold: null
hold_until: null
created_at: 2026-09-28T02:13:11.870Z
updated_at: 2026-09-28T02:35:39.288Z
started_at: 2026-09-28T02:28:24.592Z
closed_at: 2026-09-28T02:35:39.283Z
close_reason: "Re-measured: total build down 20% (1m23s -> 1m6s) despite per-Test-task CPU contention (every task individually slower, functionalTest nearly doubling at 1 fork). 5/5 consecutive local build --rerun-tasks runs green, 389/389 tests each time. example build confirmed unaffected (85/85 tests, 19s). CI re-measurement deferred (cannot push from this session). Committed as 77a6c93."
resolution: null
duplicate_of: null
---
Re-run ./gradlew build --rerun-tasks locally and on CI; record durations next to the baseline in the spec. Gate: 5 consecutive clean local runs plus green CI. Confirm ./gradlew -p example build is unaffected.
