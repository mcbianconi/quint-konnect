---
type: is
id: is-01m3dzh39fs71vn26cngefs9y4
title: Upgrade Kotlin, KSP, kotlinx-serialization and Gradle
kind: chore
status: closed
priority: 1
version: 7
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3dzgwztnpzvh73fftntn07e
  - type: blocks
    target: is-01m3e7dfmwx3g40p4shw07ke7b
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T04:28:14.510Z
updated_at: 2026-09-26T06:48:52.399Z
closed_at: 2026-09-26T06:48:52.395Z
close_reason: Kotlin 2.4.20, KSP 2.3.12, kotlinx-serialization 1.11.0, JUnit 5.14.4, Gradle wrapper 9.8.0. No source changes needed. Commit zzv on build-cleanup-upgrade.
resolution: null
duplicate_of: null
---
Kotlin 2.1.21 -> 2.4.20, KSP 2.1.21-2.0.1 -> 2.3.12 (independent versioning, verify compatibility), serialization 1.8.1 -> 1.11.0, Gradle 9.2.1 -> 9.8.0. Note: the Gradle MCP update check wrongly reports no updates.
