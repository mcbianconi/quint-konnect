---
type: is
id: is-01m3dzgt8hxnac05v85q3qp4sh
title: "Negative MBT test: prove a buggy implementation is caught"
kind: task
status: closed
priority: 1
version: 3
delegate: claude-code@macmurillo.local
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
hold: null
hold_until: null
created_at: 2026-09-26T04:28:05.264Z
updated_at: 2026-09-26T04:43:07.335Z
started_at: 2026-09-26T04:33:34.665Z
closed_at: 2026-09-26T04:43:07.334Z
close_reason: "Committed on negative-test-and-ci: buggy-winner MBT negative test (passes, reliably catches bug) and CI workflow (YAML validated, not yet run on GitHub)"
resolution: null
duplicate_of: null
---
Add an example driver with a deliberate bug and assert the runner fails on state divergence.
