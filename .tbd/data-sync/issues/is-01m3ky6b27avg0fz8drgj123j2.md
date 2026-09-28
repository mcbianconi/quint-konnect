---
type: is
id: is-01m3ky6b27avg0fz8drgj123j2
title: "CI: parallel test forks race on quint's first-run Rust evaluator download"
kind: bug
status: closed
priority: 1
version: 3
delegate: claude-code@vm
labels: []
dependencies: []
hold: null
hold_until: null
created_at: 2026-09-28T12:00:20.038Z
updated_at: 2026-09-28T12:07:50.347Z
started_at: 2026-09-28T12:00:25.591Z
closed_at: 2026-09-28T12:07:50.344Z
close_reason: CI step 'Fetch quint's Rust evaluator' (ci.yml, release.yml) downloads the evaluator before the build; both CI runs on 718560e green, the step taking ~1s.
resolution: null
duplicate_of: null
---
quint 0.32.0 downloads its Rust evaluator to ~/.quint/rust-evaluator-v0.6.0 on the first `quint run` (dist/src/rust/binaryManager.js), with no lock between processes. CI starts with no ~/.quint, and since qk-5cpb integration-tests runs classes in parallel forks, so two first-time `quint run`s race on the download and one exits non-zero without 'Invariant violated' (UnsafeCounterInvariantTest.kt:26 or EscapingCounterDriverQuintRunTest initializationError). Seen on PR #13 (0a332c9, 863ae3d). Fix: fetch the evaluator once in a CI step before the build (ci.yml and release.yml).
