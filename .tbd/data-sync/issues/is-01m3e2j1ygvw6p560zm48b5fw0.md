---
type: is
id: is-01m3e2j1ygvw6p560zm48b5fw0
title: Eval the skill on a scratch Kotlin project
kind: task
status: open
priority: 2
version: 2
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3e2j2kdtvex13knhqjv1703
parent_id: is-01m3dzfsn7btzx8cswvs0rg65t
created_at: 2026-09-26T05:21:11.631Z
updated_at: 2026-09-26T05:21:12.301Z
---
Fresh agent with only the skill wires a driver + state for a small spec until ./gradlew test passes with quint 0.32.0 (use includeBuild or publishToMavenLocal before 0.1.0). Negative case: buggy driver (commit 7e95ae3) failure output and seed are read correctly. Use anthropic-skills:skill-creator to run evals and tune the description for triggering.
