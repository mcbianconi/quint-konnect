---
type: is
id: is-01m3jvwgzvywep10dcef17jwd3
title: Post-0.2.0 assessment fixes
kind: epic
status: in_progress
priority: 1
version: 11
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
delegate: claude-code@vm
labels:
  - roadmap
  - assessment
dependencies: []
child_order_hints:
  - is-01m3jvx9b3e1bemkg8mjv156jy
  - is-01m3jvx9t2yjscdypxbxdj1d2j
  - is-01m3jvxa6f7c7wqkh45swmmh4v
  - is-01m3jvxakc7desgmn98yrhs2r8
  - is-01m3jvxaytxv2pp08pnx2rkw48
  - is-01m3jvxba17gv8xfrd824tq1xc
  - is-01m3jvxbra50zc3hhfsm1fgvqn
  - is-01m3jvxc47130tgtmpcwdv1s3k
  - is-01m3jvxcejc3bt8rx60emxxynq
hold: null
hold_until: null
created_at: 2026-09-28T02:00:46.841Z
updated_at: 2026-09-28T02:17:47.838Z
started_at: 2026-09-28T02:17:47.838Z
---
Fixes from the project assessment of main at aa80813 (v0.2.0): stale traces, same-name trace directories, all-driver trace generation, missing time limits, duplicate command builder, dead parallelism option, doc drift. Full design and evidence: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md. Do B1-B6 one after the other (they change the same gradle-plugin files); B7, B8, B9 can start at any time. CLAUDE.md rule: a change to the Gradle plugin, Driver, annotations or -Pquint.* properties must also update skills/quint-konnect/.
