---
type: is
id: is-01m3e5ab6t9qhmv0vvc22d8t99
title: Move KSP generators to KotlinPoet
kind: chore
status: open
priority: 2
version: 4
labels:
  - roadmap
dependencies:
  - type: blocks
    target: is-01m3dzgt191nqmn1w46g6e12c9
  - type: blocks
    target: is-01m3e5acv70a2rdn8yk60vgwg9
  - type: blocks
    target: is-01m3e5adry6kfvhf6h1drk10bj
parent_id: is-01m3e5a9sj92e7ptv02j419tm8
created_at: 2026-09-26T06:09:24.697Z
updated_at: 2026-09-26T06:49:24.324Z
---
StepMethodGenerator builds code with appendLine; typeName() drops in/out projections and likely breaks on typealiases, value classes, inner classes. qk-gu38 was an escaping bug from this. Use KotlinPoet + toTypeName().
