---
type: is
id: is-01m3jm95saj2s5c0gh6d9vmgat
title: Confirm/land the CI gate
kind: task
status: open
priority: 2
version: 2
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3jm960y58d5swkkv3aw8e9p
parent_id: is-01m3jm7b8yafz6jr653btzjykf
created_at: 2026-09-27T23:47:52.745Z
updated_at: 2026-09-27T23:48:05.953Z
---
ktlint Gradle plugins typically wire ktlintCheck into check by default, so './gradlew build' (root, .github/workflows/ci.yml) and './gradlew -p example build' may already gate once qk-fhdg is fully wired in and qk-lafs has landed. Confirm both CI jobs actually fail on a violation (introduce a deliberate one locally and check) rather than assuming a new CI step is required; only add an explicit step if the default wiring doesn't cover it.
