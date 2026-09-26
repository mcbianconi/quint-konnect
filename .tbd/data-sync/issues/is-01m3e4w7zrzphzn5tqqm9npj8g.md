---
type: is
id: is-01m3e4w7zrzphzn5tqqm9npj8g
title: "@QuintTest cannot replay: quint test emits no mbt:: variables"
kind: bug
status: in_progress
priority: 1
version: 2
labels:
  - roadmap
dependencies: []
parent_id: is-01m3dzfse7fsb3drhmwz7h0sn9
created_at: 2026-09-26T06:01:42.648Z
updated_at: 2026-09-26T06:12:06.590Z
---
Found while fixing qk-gu38 (quint 0.32.0): quint test does not write mbt::actionTaken / mbt::nondetPicks, and TestConfig passes no --mbt, so Step.fromState fails with 'Missing mbt::actionTaken variable' for every generated @QuintTest. No example uses @QuintTest. Check whether quint test accepts --mbt; otherwise derive the step another way or remove @QuintTest before 0.1.0.
