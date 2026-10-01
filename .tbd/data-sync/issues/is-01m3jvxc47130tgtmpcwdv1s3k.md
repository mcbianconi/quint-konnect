---
type: is
id: is-01m3jvxc47130tgtmpcwdv1s3k
title: Decide the readSpecIr default before 1.0
kind: task
status: open
priority: 4
version: 5
spec_path: docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md
delegate: null
labels:
  - roadmap
  - assessment
dependencies: []
parent_id: is-01m3jvwgzvywep10dcef17jwd3
hold: null
hold_until: null
created_at: 2026-09-28T02:01:14.631Z
updated_at: 2026-10-01T02:58:19.220Z
started_at: 2026-09-28T02:17:47.863Z
---
Spec section B8 (docs/project/specs/active/plan-2026-09-28-post-0.2.0-assessment-fixes.md).
The main value (generated <Module>Spec types, compile-time spec checks) needs readSpecIr, whose default is false (gradle-plugin/.../QuintKonnectExtension.kt). Tests already need quint. Cost: -Pquint.replay then also needs quint (docs/decisions/replay-needs-quint-with-read-spec-ir.md).
Output: a decision record in docs/decisions/ (plus its README index line). Change the default only if the decision accepts it; then update README and skills/quint-konnect/.

## Notes

Restart 2026-09-30: claim cleared; no decision record written yet. Design decision still stands (user 2026-09-28): change readSpecIr default to true. Record in docs/decisions/, update README and skills/quint-konnect/.
