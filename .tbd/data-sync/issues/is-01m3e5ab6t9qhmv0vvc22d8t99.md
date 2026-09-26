---
type: is
id: is-01m3e5ab6t9qhmv0vvc22d8t99
title: Move KSP generators to KotlinPoet
kind: chore
status: open
priority: 2
version: 1
labels:
  - roadmap
dependencies: []
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:24.697Z
updated_at: 2026-09-26T06:09:24.697Z
---
StepMethodGenerator builds code with appendLine; typeName() drops in/out projections and likely breaks on typealiases, value classes, inner classes. qk-gu38 was an escaping bug from this. Use KotlinPoet + toTypeName().
