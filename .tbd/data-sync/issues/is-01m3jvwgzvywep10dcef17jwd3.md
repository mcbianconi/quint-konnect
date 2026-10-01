---
type: is
id: is-01m3jvwgzvywep10dcef17jwd3
title: Post-0.2.0 assessment fixes
kind: epic
status: open
priority: 1
version: 13
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
delegate: null
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
updated_at: 2026-10-01T02:58:18.548Z
started_at: 2026-09-28T02:17:47.838Z
---
Fixes from the project assessment of main at aa80813 (v0.2.0): stale traces, same-name trace directories, all-driver trace generation, missing time limits, duplicate command builder, dead parallelism option, doc drift. Full design and evidence: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md. Do B1-B6 one after the other (they change the same gradle-plugin files); B7, B8, B9 can start at any time. CLAUDE.md rule: a change to the Gradle plugin, Driver, annotations or -Pquint.* properties must also update skills/quint-konnect/.

## Notes

Restart 2026-09-30: abandoned mid-epic. Cleared all in_progress claims.

Landed on origin/main: none of B1–B9. Spec + design decisions recorded:
- B3 (qk-blt0): generateQuintTraces opt-in, default off; test-time TraceGenerator default; no checkQuint on Test tasks without drivers.
- B6 (qk-5fyu): remove ReplayRunner.runTest too; callers move to traceReplays.
- B8 (qk-zu8a): change readSpecIr default to true (decision record still needed).

Unlanded local GitButler work (not on main; treat as discardable reference):
- B1 qk-q5z8 on fix/qk-q5z8-traces-spec-input
- B2 qk-q6av on fix/qk-q6av-trace-dir-per-driver (stacked on B1)

Also present locally (separate from this epic): .cursor/skills/verify-quint-konnect/ verification skill; GitButler branch pstack (chore(ai): pstack setup).
