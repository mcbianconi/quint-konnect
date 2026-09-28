---
type: is
id: is-01m3jm95saj2s5c0gh6d9vmgat
title: Confirm/land the CI gate
kind: task
status: closed
priority: 2
version: 4
delegate: claude-code@vm
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3jm960y58d5swkkv3aw8e9p
parent_id: is-01m3jm7b8yafz6jr653btzjykf
hold: null
hold_until: null
created_at: 2026-09-27T23:47:52.745Z
updated_at: 2026-09-28T11:38:50.381Z
started_at: 2026-09-28T11:36:06.137Z
closed_at: 2026-09-28T11:38:50.381Z
close_reason: "spotlessCheck now wired into check (isEnforceCheck default). Verified: deliberate violations in core/src/main, example/src/test and build-logic/src each fail ./gradlew build (the CI Build step); -p example build doesn't lint on its own but runs after the root build in the same job, so no extra CI step."
resolution: null
duplicate_of: null
---
ktlint Gradle plugins typically wire ktlintCheck into check by default, so './gradlew build' (root, .github/workflows/ci.yml) and './gradlew -p example build' may already gate once qk-fhdg is fully wired in and qk-lafs has landed. Confirm both CI jobs actually fail on a violation (introduce a deliberate one locally and check) rather than assuming a new CI step is required; only add an explicit step if the default wiring doesn't cover it.
